package com.voicecal.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * LLM 增强 NLU 服务 — 接入 DeepSeek API
 *
 * 架构：规则引擎(优先) → DeepSeek LLM(降级) → UNKNOWN(兜底)
 * 当规则引擎置信度 < 0.6 时，调用 LLM 进行更准确的意图+实体提取
 */
@Service
public class LlmNluService {

    private static final Logger log = LoggerFactory.getLogger(LlmNluService.class);

    private final DeepSeekConfig deepSeekConfig;
    private final ObjectMapper objectMapper;

    /** 置信度阈值：低于此值时调用 LLM */
    public static final double CONFIDENCE_THRESHOLD = 0.6;

    private static final String SYSTEM_PROMPT = """
            你是日历系统的自然语言理解模块。从用户中文输入中提取意图和日期信息。

            今天是：%s（%s）

            【意图判断】
            QUERY: 查询/查看日程（"有什么安排"、"看看日程"、"查查"、"显示"、"今天"、"明天"等单独出现也视为查询）
            CREATE: 创建事件（"创建"、"新建"、"添加"、"安排一个"、"加个"）
            UPDATE: 修改事件（"修改"、"更改"、"推迟"、"提前"）
            DELETE: 删除事件（"删除"、"取消"、"移除"）
            REMINDER: 设置提醒（"提醒"、"记得"、"别忘"）
            UNKNOWN: 无法识别

            【关键区分】
            "有什么安排" → QUERY（绝对不是CREATE）
            "安排一个会议" → CREATE
            "今天有什么事" → QUERY
            "明天下午开会" → CREATE

            【日期解析规则】
            1. 所有相对日期必须转为绝对日期（yyyy-MM-dd格式）
            2. "大后天"=今天+3天，"后天"=今天+2天，"明天"=今天+1天
            3. "本周"=startDate:本周一, endDate:本周日（范围查询）
            4. "这个月"=startDate:本月1号, endDate:本月最后一天
            5. "下周末"=startDate:下周六, endDate:下周日
            6. "月底"=endDate:本月最后一天
            7. "五月三十一号"→2026-05-31，"十五号"→当月15号
            8. "三天后"=今天+3天
            9. "周X"如果没有"上周"或"下周"前缀，取本周（如果已过则取下周）
            10. QUERY意图如果没有明确日期，默认查今天

            【分类】
            work: 工作/会议/开会/面试/出差/培训
            family: 家庭/家人/父母/孩子
            personal: 个人/运动/健身/看医生

            【输出格式】只输出JSON，不要其他内容：
            {"intent":"QUERY","entities":{"title":null,"date":"2026-05-31","startDate":null,"endDate":null,"startTime":null,"endTime":null,"category":null},"confidence":0.95}
            """;

    public LlmNluService(DeepSeekConfig deepSeekConfig, ObjectMapper objectMapper) {
        this.deepSeekConfig = deepSeekConfig;
        this.objectMapper = objectMapper;
    }

    /**
     * 使用 DeepSeek LLM 解析自然语言
     * @return NluResult，解析失败时返回 null
     */
    public NluResult parse(String text) {
        try {
            LocalDate today = LocalDate.now();
            String todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String[] weekdays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
            String weekday = weekdays[today.getDayOfWeek().getValue() - 1];
            String todayFull = todayStr + " " + weekday;

            String systemPrompt = String.format(SYSTEM_PROMPT, todayStr, todayFull);

            String llmResponse = callDeepSeek(systemPrompt, text);
            if (llmResponse == null) return null;

            return parseLlmResponse(text, llmResponse);
        } catch (Exception e) {
            log.error("DeepSeek NLU 解析失败", e);
            return null;
        }
    }

    /**
     * 调用 DeepSeek Chat Completions API（OpenAI 兼容格式）
     */
    private String callDeepSeek(String systemPrompt, String userMessage) {
        HttpURLConnection conn = null;
        try {
            String url = deepSeekConfig.getApiUrl();
            String apiKey = deepSeekConfig.getApiKey();
            String model = deepSeekConfig.getModel();

            // 构建 OpenAI 兼容的请求体
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", "请解析以下用户输入，严格输出JSON：\n" + userMessage)
                    ),
                    "temperature", 0.1,
                    "max_tokens", 500,
                    "response_format", Map.of("type", "json_object")
            );

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            log.debug("DeepSeek 请求: model={}, text={}", model, userMessage);

            URI uri = new URI(url);
            conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                byte[] errorBytes = conn.getErrorStream() != null
                        ? conn.getErrorStream().readAllBytes() : new byte[0];
                log.error("DeepSeek API 错误: HTTP {}, body={}", responseCode,
                        new String(errorBytes, StandardCharsets.UTF_8));
                return null;
            }

            byte[] responseBytes = conn.getInputStream().readAllBytes();
            JsonNode json = objectMapper.readTree(responseBytes);

            // OpenAI 格式响应：choices[0].message.content
            if (json.has("choices") && json.get("choices").isArray() && json.get("choices").size() > 0) {
                String content = json.get("choices").get(0).get("message").get("content").asText();
                log.debug("DeepSeek 响应: {}", content);
                return content;
            }

            log.warn("DeepSeek 响应格式异常: {}", json);
            return null;
        } catch (Exception e) {
            log.error("调用 DeepSeek API 失败", e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * 解析 LLM 返回的 JSON 为 NluResult
     */
    private NluResult parseLlmResponse(String rawText, String llmResponse) {
        try {
            // 提取 JSON（LLM 可能包裹在 markdown 代码块中）
            String jsonStr = extractJson(llmResponse);
            JsonNode json = objectMapper.readTree(jsonStr);

            String intent = json.has("intent") ? json.get("intent").asText("UNKNOWN") : "UNKNOWN";
            double confidence = json.has("confidence") ? json.get("confidence").asDouble(0.5) : 0.5;

            NluResult result = new NluResult(rawText, intent, confidence, "llm");

            // 解析实体
            if (json.has("entities") && json.get("entities").isObject()) {
                JsonNode entities = json.get("entities");
                addEntityIfPresent(result, entities, "title");
                addEntityIfPresent(result, entities, "date");
                addEntityIfPresent(result, entities, "startDate");
                addEntityIfPresent(result, entities, "endDate");
                addEntityIfPresent(result, entities, "startTime");
                addEntityIfPresent(result, entities, "endTime");
                addEntityIfPresent(result, entities, "category");
            }

            // QUERY 意图：确保有日期
            if ("QUERY".equals(intent)) {
                if (!result.getEntities().containsKey("startDate") && !result.getEntities().containsKey("date")) {
                    result.addEntity("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
                }
            }

            log.info("DeepSeek NLU 解析结果: intent={}, confidence={}, entities={}",
                    intent, confidence, result.getEntities());

            return result;
        } catch (Exception e) {
            log.error("解析 DeepSeek 响应失败: {}", llmResponse, e);
            return null;
        }
    }

    /**
     * 安全提取实体字段
     */
    private void addEntityIfPresent(NluResult result, JsonNode entities, String key) {
        if (entities.has(key) && !entities.get(key).isNull()) {
            String value = entities.get(key).asText();
            if (value != null && !value.isEmpty() && !"null".equals(value)) {
                result.addEntity(key, value);
            }
        }
    }

    /**
     * 从 LLM 响应中提取 JSON 字符串
     */
    private String extractJson(String text) {
        String trimmed = text.trim();
        if (trimmed.contains("```json")) {
            trimmed = trimmed.substring(trimmed.indexOf("```json") + 7);
            trimmed = trimmed.substring(0, trimmed.indexOf("```"));
        } else if (trimmed.contains("```")) {
            trimmed = trimmed.substring(trimmed.indexOf("```") + 3);
            trimmed = trimmed.substring(0, trimmed.indexOf("```"));
        }
        return trimmed.trim();
    }
}
