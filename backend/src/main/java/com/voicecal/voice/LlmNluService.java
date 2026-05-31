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
import java.time.LocalDateTime;
import java.time.LocalTime;
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
            你是日历系统的自然语言理解模块。从用户中文输入中提取意图和完整字段信息。

            今天是：%s（%s）

            【意图判断 - 多维度分析】

            QUERY: 查询/查看日程（只读操作）
            - 有询问性关键词："有什么"、"看看"、"查查"、"显示"、"查看"、"有没有"
            - 模糊表达（只有日期+事件名，无具体时间）："明天开会"、"下周三培训"
            - 例句："今天有什么安排"、"明天看看日程"、"下周三有没有会议"、"明天开会"

            CREATE: 创建事件（写入操作）
            - 有动作动词："创建"、"新建"、"添加"、"新增"、"安排一个"、"加一个"、"设一个"
            - 有具体时间（表示创建意图）："明天下午三点开会"、"下周三十点培训"
            - 例句："创建一个会议"、"明天下午三点开会"、"新建明天的项目评审"、"安排一个会议"

            UPDATE: 修改事件（"修改"、"更改"、"推迟"、"提前"）
            DELETE: 删除事件（"删除"、"取消"、"移除"）
            REMINDER: 设置提醒（"提醒"、"记得"、"别忘"）
            UNKNOWN: 无法识别

            【关键区分规则 - 必须严格遵守】
            1. 有"创建/新建/添加/安排一个/加一个"等动作动词 → CREATE
            2. 有具体时间（"下午三点"、"10:00"、"上午"、"下午"、"晚上"）+ 事件名 → CREATE
            3. 只有日期+事件名（无具体时间）→ QUERY
            4. 有询问性词汇（"有什么"、"看看"）→ QUERY
            5. "安排"单独出现（如"有什么安排"）→ QUERY
            6. "安排一个"出现 → CREATE

            【正确示例】
            "明天下午三点开会" → CREATE（有具体时间"下午三点"）
            "下周三十点培训" → CREATE（有具体时间"十点"）
            "创建一个会议" → CREATE（有动作动词"创建"）
            "新建明天的项目评审" → CREATE（有动作动词"新建"）
            "明天开会" → QUERY（模糊表达，无具体时间）
            "今天有什么事" → QUERY（有询问性词汇"有什么"）
            "明天有什么安排" → QUERY（有询问性词汇"有什么"）

            【CREATE 意图完整字段提取】
            必须提取：
            - title: 事件标题（从动词后的名词提取，如"创建会议"→"会议"，"开产品讨论会"→"产品讨论会"）
            - date: 日期（yyyy-MM-dd格式，所有相对日期必须转为绝对日期）
            - startTime: 开始时间（HH:mm格式）
            - endTime: 结束时间（HH:mm格式，默认startTime + 1小时）

            尽量提取：
            - allDay: 是否全天事件（布尔值，如"全天团建"→true）
            - location: 地点（如"在会议室A"→"会议室A"）
            - description: 描述信息
            - category: 分类（根据内容推断）
            - color: 颜色（根据分类推断）
            - priority: 优先级（0=普通, 1=重要, 2=紧急）

            【时间解析规则】
            1. "下午三点" → 15:00
            2. "上午十点" → 10:00
            3. "晚上八点" → 20:00
            4. "三点" → 03:00（默认凌晨，除非有"下午"前缀）
            5. "15:00" → 15:00
            6. "下午3点半" → 15:30

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

            【分类推断规则】
            work: 工作/会议/开会/面试/出差/培训/项目/评审
            family: 家庭/家人/父母/孩子
            personal: 个人/运动/健身/看医生/看病
            social: 聚餐/聚会/生日/纪念日/团建

            【颜色推断规则】
            work → #6366f1（靛蓝）
            personal → #22c55e（绿色）
            health → #f59e0b（琥珀）
            social → #ec4899（粉色）

            【输出格式】只输出JSON，不要其他内容：
            {"intent":"CREATE","entities":{"title":"项目评审","date":"2026-06-01","startTime":"15:00","endTime":"16:00","allDay":false,"location":"会议室A","description":null,"category":"work","color":"#6366f1","priority":0},"confidence":0.95,"missingFields":[]}

            【重要 - 关键字优先匹配规则】
            如果用户输入包含以下关键字，必须优先匹配对应意图：
            - 包含"创建"或"新建"或"添加" → CREATE
            - 包含"查询"或"查看"或"看看"或"有什么" → QUERY
            - 包含"修改"或"更改"或"编辑" → UPDATE
            - 包含"删除"或"取消"或"移除" → DELETE
            - 包含"提醒" → REMINDER
            注意：关键字匹配优先级最高，即使句子中包含其他词汇也以关键字为准
            """;

    /**
     * 实体提取专用 Prompt - 用户已选择意图，LLM 只提取实体
     */
    private static final String EXTRACT_ENTITIES_PROMPT = """
            你是日历系统的实体提取模块。用户已选择意图，你只需要从用户输入中提取实体信息。

            今天是：%s（%s）
            用户选择的意图：%s

            【任务】
            从用户输入中提取以下实体（不判断意图，意图已确定）：

            【CREATE 意图需要提取】
            - title: 事件标题（必须）
            - date: 日期（yyyy-MM-dd格式，默认明天）
            - startTime: 开始时间（HH:mm格式，默认09:00）
            - endTime: 结束时间（HH:mm格式，默认startTime + 1小时）
            - location: 地点（可选）
            - category: 分类（work/personal/health/social，可选）
            - allDay: 是否全天事件（布尔值，可选）

            【QUERY 意图需要提取】
            - date: 查询日期（yyyy-MM-dd格式，默认今天）
            - startDate: 开始日期（范围查询）
            - endDate: 结束日期（范围查询）
            - category: 分类过滤（可选）

            【UPDATE 意图需要提取】
            - title: 要修改的事件标题（必须）
            - date: 事件日期（可选，默认今天）
            - newStartTime: 新开始时间（HH:mm格式，可选）
            - newEndTime: 新结束时间（HH:mm格式，可选）
            - newDate: 新日期（可选）

            【DELETE 意图需要提取】
            - title: 要删除的事件标题（必须）
            - date: 事件日期（可选，默认今天）

            【REMINDER 意图需要提取】
            - title: 事件标题（必须，默认"提醒事项"）
            - date: 事件日期（可选，默认明天）
            - startTime: 开始时间（HH:mm格式，默认09:00）
            - remindBefore: 提前提醒分钟数（可选，默认15）

            【日期解析规则】
            1. "明天" = 今天 + 1天
            2. "后天" = 今天 + 2天
            3. "下周三" = 下周的周三
            4. "下午三点" = 15:00
            5. "上午十点" = 10:00

            【输出格式】只输出JSON，不要其他内容：
            {"title":"会议","date":"2026-06-01","startTime":"15:00","endTime":"16:00","newStartTime":"16:00","location":null,"category":"work","allDay":false}
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
     * 提取实体 - 用户已选择意图，LLM 只提取实体
     * @param intent 用户选择的意图（CREATE/QUERY/UPDATE/DELETE）
     * @param text 用户语音输入
     * @return NluResult，解析失败时返回 null
     */
    public NluResult extractEntities(String intent, String text) {
        try {
            LocalDate today = LocalDate.now();
            String todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String[] weekdays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
            String weekday = weekdays[today.getDayOfWeek().getValue() - 1];
            String todayFull = todayStr + " " + weekday;

            String systemPrompt = String.format(EXTRACT_ENTITIES_PROMPT, todayStr, todayFull, intent);

            String llmResponse = callDeepSeek(systemPrompt, text);
            if (llmResponse == null) return null;

            return parseExtractResponse(text, intent, llmResponse);
        } catch (Exception e) {
            log.error("DeepSeek 实体提取失败", e);
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
                    "max_tokens", 2000,
                    "response_format", Map.of("type", "json_object")
            );

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            log.debug("DeepSeek 请求: model={}, text={}", model, userMessage);

            URI uri = new URI(url);
            conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

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
                addEntityIfPresent(result, entities, "location");
                addEntityIfPresent(result, entities, "description");
                addEntityIfPresent(result, entities, "color");

                // 布尔和数字类型
                if (entities.has("allDay") && !entities.get("allDay").isNull()) {
                    result.addEntity("allDay", entities.get("allDay").asBoolean(false));
                }
                if (entities.has("priority") && !entities.get("priority").isNull()) {
                    result.addEntity("priority", entities.get("priority").asInt(0));
                }
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
     * 解析实体提取响应
     */
    private NluResult parseExtractResponse(String rawText, String intent, String llmResponse) {
        try {
            String jsonStr = extractJson(llmResponse);
            JsonNode json = objectMapper.readTree(jsonStr);

            NluResult result = new NluResult(rawText, intent, 0.95, "llm_extract");

            // 直接解析 JSON 中的实体（不嵌套在 entities 字段中）
            addEntityIfPresent(result, json, "title");
            addEntityIfPresent(result, json, "date");
            addEntityIfPresent(result, json, "startDate");
            addEntityIfPresent(result, json, "endDate");
            addEntityIfPresent(result, json, "startTime");
            addEntityIfPresent(result, json, "endTime");
            addEntityIfPresent(result, json, "newStartTime");
            addEntityIfPresent(result, json, "newEndTime");
            addEntityIfPresent(result, json, "newDate");
            addEntityIfPresent(result, json, "location");
            addEntityIfPresent(result, json, "category");
            addEntityIfPresent(result, json, "description");

            // 布尔和数字类型
            if (json.has("allDay") && !json.get("allDay").isNull()) {
                result.addEntity("allDay", json.get("allDay").asBoolean(false));
            }
            if (json.has("priority") && !json.get("priority").isNull()) {
                result.addEntity("priority", json.get("priority").asInt(0));
            }

            // 为 CREATE 意图设置默认值
            if ("CREATE".equals(intent)) {
                if (!result.getEntities().containsKey("date")) {
                    result.addEntity("date", LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
                }
                if (!result.getEntities().containsKey("startTime")) {
                    result.addEntity("startTime", "09:00");
                }
                if (!result.getEntities().containsKey("endTime")) {
                    String startStr = (String) result.getEntities().get("startTime");
                    LocalTime start = LocalTime.parse(startStr);
                    result.addEntity("endTime", start.plusHours(1).format(DateTimeFormatter.ofPattern("HH:mm")));
                }
            }

            // 为 QUERY 意图设置默认值
            if ("QUERY".equals(intent)) {
                if (!result.getEntities().containsKey("date") && !result.getEntities().containsKey("startDate")) {
                    result.addEntity("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
                }
            }

            log.info("实体提取结果: intent={}, entities={}", intent, result.getEntities());

            return result;
        } catch (Exception e) {
            log.error("解析实体提取响应失败: {}", llmResponse, e);
            return null;
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
