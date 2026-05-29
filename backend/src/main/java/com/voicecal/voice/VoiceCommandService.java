package com.voicecal.voice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicecal.common.ApiResult;
import com.voicecal.entity.Festival;
import com.voicecal.entity.FestivalGreetingLog;
import com.voicecal.entity.VoiceCommandLog;
import com.voicecal.mapper.FestivalMapper;
import com.voicecal.mapper.FestivalGreetingLogMapper;
import com.voicecal.service.EventService;
import com.voicecal.service.VoiceCommandLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VoiceCommandService {

    private static final Logger log = LoggerFactory.getLogger(VoiceCommandService.class);

    private final BaiduAsrService asrService;
    private final BaiduTtsService ttsService;
    private final VoiceNluService nluService;
    private final EventService eventService;
    private final FestivalMapper festivalMapper;
    private final FestivalGreetingLogMapper greetingLogMapper;
    private final VoiceCommandLogService voiceCommandLogService;
    private final ObjectMapper objectMapper;

    public VoiceCommandService(BaiduAsrService asrService,
                                BaiduTtsService ttsService,
                                VoiceNluService nluService,
                                EventService eventService,
                                FestivalMapper festivalMapper,
                                FestivalGreetingLogMapper greetingLogMapper,
                                VoiceCommandLogService voiceCommandLogService,
                                ObjectMapper objectMapper) {
        this.asrService = asrService;
        this.ttsService = ttsService;
        this.nluService = nluService;
        this.eventService = eventService;
        this.festivalMapper = festivalMapper;
        this.greetingLogMapper = greetingLogMapper;
        this.voiceCommandLogService = voiceCommandLogService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> processTextCommand(Long userId, String text) {
        long startTime = System.currentTimeMillis();
        NluResult nluResult = nluService.parse(text);

        Map<String, Object> response = new HashMap<>();
        response.put("text", text);
        response.put("intent", nluResult.getIntent());
        response.put("entities", nluResult.getEntities());
        response.put("confidence", nluResult.getConfidence());
        response.put("nluSource", nluResult.getNluSource());
        response.put("requiresClarify", nluResult.isRequiresClarify());
        response.put("clarifyQuestion", nluResult.getClarifyQuestion());

        String commandResult = "success";
        String responseText = "好的，已为您";
        try {
            switch (nluResult.getIntent()) {
                case "CREATE" -> {
                    var req = buildCreateRequest(nluResult);
                    response.put("action", Map.of("type", "preview", "event", req));
                    responseText = "已为您创建事件「" + req.get("title") + "」";
                }
                case "QUERY" -> {
                    responseText = "正在为您查询日历...";
                }
                case "UPDATE" -> {
                    responseText = "好的，已为您更新事件";
                }
                case "DELETE" -> {
                    responseText = "好的，已为您删除事件";
                }
                case "REMINDER" -> {
                    responseText = "好的，已为您设置提醒";
                }
                default -> {
                    commandResult = "failed";
                    responseText = "抱歉，我没有理解您的指令";
                }
            }
        } catch (Exception e) {
            commandResult = "failed";
            responseText = "处理失败，请重试";
            log.error("语音命令处理失败: userId={}, text={}", userId, text, e);
        }

        response.put("responseText", responseText);
        response.put("action", response.getOrDefault("action", null));

        long duration = System.currentTimeMillis() - startTime;

        VoiceCommandLog logEntry = new VoiceCommandLog();
        logEntry.setUserId(userId);
        logEntry.setRawAudioText(text);
        logEntry.setIntent(nluResult.getIntent());
        try {
            logEntry.setEntities(objectMapper.writeValueAsString(nluResult.getEntities()));
        } catch (Exception e) {
            logEntry.setEntities("{}");
        }
        logEntry.setConfidence(BigDecimal.valueOf(nluResult.getConfidence()));
        logEntry.setNluSource(nluResult.getNluSource());
        logEntry.setCommandResult(commandResult);
        logEntry.setResponseText(responseText);
        logEntry.setDurationMs((int) duration);
        voiceCommandLogService.log(logEntry);

        return response;
    }

    public ApiResult<Map<String, Object>> handleFestivalGreeting(Long userId, Map<String, Object> body) {
        try {
            Object festivalIdObj = body.get("festivalId");
            Object eventIdObj = body.get("eventId");

            if (festivalIdObj == null) {
                return ApiResult.badRequest("festivalId 不能为空");
            }

            Long festivalId = Long.valueOf(festivalIdObj.toString());
            Festival festival = festivalMapper.selectById(festivalId);

            if (festival == null) {
                return ApiResult.notFound("节日不存在");
            }

            String greetingText = festival.getGreeting();
            String suggestions = festival.getSuggestions();

            String fullText = greetingText;
            if (suggestions != null && !suggestions.isBlank()) {
                try {
                    String[] suggestionList = objectMapper.readValue(suggestions, String[].class);
                    if (suggestionList.length > 0) {
                        fullText += " 建议您：" + suggestionList[0];
                    }
                } catch (Exception ignored) {}
            }

            byte[] audioData = ttsService.synthesize(fullText, "female", 5, 5);

            FestivalGreetingLog greetingLog = new FestivalGreetingLog();
            greetingLog.setUserId(userId);
            greetingLog.setFestivalId(festivalId);
            greetingLog.setEventId(eventIdObj != null ? Long.valueOf(eventIdObj.toString()) : null);
            greetingLog.setGreetingType("first_create");
            greetingLog.setGreetingText(fullText);
            greetingLog.setIsTtsSent(true);
            greetingLog.setUserAction("accept");
            greetingLogMapper.insert(greetingLog);

            Map<String, Object> data = new HashMap<>();
            data.put("greetingText", fullText);
            data.put("audioBase64", Base64.getEncoder().encodeToString(audioData));
            data.put("durationMs", fullText.length() * 200);

            return ApiResult.success(data);
        } catch (Exception e) {
            log.error("节日关怀 TTS 失败: userId={}", userId, e);
            return ApiResult.error(com.voicecal.common.ResultCode.TTS_SERVICE_ERROR);
        }
    }

    private Map<String, Object> buildCreateRequest(NluResult nluResult) {
        Map<String, Object> req = new HashMap<>();
        req.put("title", nluResult.getEntities().getOrDefault("title", "新事件"));
        req.put("category", nluResult.getEntities().getOrDefault("category", "personal"));

        String date = (String) nluResult.getEntities().get("date");
        String startTime = (String) nluResult.getEntities().get("startTime");
        String endTime = (String) nluResult.getEntities().get("endTime");

        LocalDate d = date != null ? LocalDate.parse(date) : LocalDate.now();
        LocalTime st = startTime != null ? LocalTime.parse(startTime) : LocalTime.of(9, 0);
        LocalTime et = endTime != null ? LocalTime.parse(endTime) : st.plusHours(1);

        req.put("startTime", d.atTime(st).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        req.put("endTime", d.atTime(et).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return req;
    }

}
