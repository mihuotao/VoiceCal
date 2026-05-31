package com.voicecal.voice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicecal.common.ApiResult;
import com.voicecal.entity.CalendarEvent;
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
import java.util.ArrayList;
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
                    Map<String, Object> queryResult = handleQuery(userId, nluResult);
                    Map<String, Object> action = new HashMap<>();
                    action.put("type", "query");
                    action.put("events", queryResult.get("events"));
                    action.put("queryDate", queryResult.get("queryDate"));
                    action.put("queryEndDate", queryResult.get("queryEndDate"));
                    response.put("action", action);
                    responseText = (String) queryResult.get("responseText");
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
                    responseText = nluResult.isRequiresClarify() && nluResult.getClarifyQuestion() != null
                            ? nluResult.getClarifyQuestion()
                            : "抱歉，我没有理解您的指令";
                    response.put("action", Map.of("type", "unknown"));
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

    private Map<String, Object> handleQuery(Long userId, NluResult nluResult) {
        Map<String, Object> result = new HashMap<>();

        // 从 NLU 实体中提取日期范围
        LocalDate startDate = parseDate((String) nluResult.getEntities().get("startDate"));
        LocalDate endDate = parseDate((String) nluResult.getEntities().get("endDate"));

        // 兼容旧逻辑：单日 date 字段
        if (startDate == null) {
            startDate = parseDate((String) nluResult.getEntities().get("date"));
        }
        if (startDate == null) startDate = LocalDate.now();
        if (endDate == null) endDate = startDate;

        // 提取分类过滤条件
        String category = (String) nluResult.getEntities().get("category");

        log.info("语音查询: userId={}, startDate={}, endDate={}, category={}, entities={}",
                userId, startDate, endDate, category, nluResult.getEntities());

        // 查询日期范围内的事件
        List<CalendarEvent> events = eventService.listByDateRange(userId, startDate, endDate);

        log.info("查询结果: 找到 {} 个事件", events.size());

        // 按分类过滤
        if (category != null && !category.isBlank()) {
            String finalCategory = category;
            events = events.stream()
                    .filter(e -> finalCategory.equals(e.getCategory()))
                    .collect(java.util.stream.Collectors.toList());
        }

        // 构建返回数据
        List<Map<String, Object>> eventList = new ArrayList<>();
        for (CalendarEvent event : events) {
            Map<String, Object> ev = new HashMap<>();
            ev.put("id", event.getId());
            ev.put("title", event.getTitle());
            ev.put("description", event.getDescription());
            ev.put("startTime", event.getStartTime() != null
                    ? event.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
            ev.put("endTime", event.getEndTime() != null
                    ? event.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
            ev.put("allDay", event.getAllDay());
            ev.put("location", event.getLocation());
            ev.put("color", event.getColor());
            ev.put("category", event.getCategory());
            ev.put("priority", event.getPriority());
            ev.put("status", event.getStatus());
            eventList.add(ev);
        }

        // 构建 TTS 播报文本
        String responseText = buildQueryResponseText(startDate, endDate, events, category);

        result.put("events", eventList);
        result.put("queryDate", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        result.put("queryEndDate", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        result.put("responseText", responseText);
        return result;
    }

    /**
     * 安全解析日期字符串
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 构建查询结果的 TTS 播报文本
     */
    private String buildQueryResponseText(LocalDate startDate, LocalDate endDate,
                                           List<CalendarEvent> events, String category) {
        String dateLabel = buildDateLabel(startDate, endDate);
        String categoryLabel = buildCategoryLabel(category);

        if (events.isEmpty()) {
            return dateLabel + categoryLabel + "没有日程安排";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(dateLabel).append(categoryLabel).append("有").append(events.size()).append("个安排：");

        for (int i = 0; i < events.size(); i++) {
            CalendarEvent ev = events.get(i);
            if (i > 0) sb.append("，");

            // 日期标签（范围查询时显示日期）
            if (!startDate.equals(endDate) && ev.getStartTime() != null) {
                LocalDate evDate = ev.getStartTime().toLocalDate();
                sb.append(evDate.getMonthValue()).append("月").append(evDate.getDayOfMonth()).append("号");
            }

            if (ev.getStartTime() != null) {
                LocalTime time = ev.getStartTime().toLocalTime();
                sb.append(time.getHour()).append("点");
                if (time.getMinute() > 0) {
                    sb.append(time.getMinute()).append("分");
                }
            }
            sb.append(ev.getTitle());
        }
        return sb.toString();
    }

    /**
     * 构建日期标签
     */
    private String buildDateLabel(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();

        if (startDate.equals(endDate)) {
            // 单日
            if (startDate.equals(today)) return "今天";
            if (startDate.equals(today.plusDays(1))) return "明天";
            if (startDate.equals(today.minusDays(1))) return "昨天";
            return startDate.getMonthValue() + "月" + startDate.getDayOfMonth() + "号";
        }

        // 范围
        if (startDate.equals(today.with(java.time.DayOfWeek.MONDAY))
                && endDate.equals(today.with(java.time.DayOfWeek.SUNDAY))) {
            return "本周";
        }
        if (startDate.getDayOfMonth() == 1 && endDate.equals(startDate.withDayOfMonth(startDate.lengthOfMonth()))) {
            return startDate.getMonthValue() + "月";
        }
        return startDate.getMonthValue() + "月" + startDate.getDayOfMonth() + "号到"
                + endDate.getMonthValue() + "月" + endDate.getDayOfMonth() + "号";
    }

    /**
     * 构建分类标签
     */
    private String buildCategoryLabel(String category) {
        if (category == null || category.isBlank()) return "";
        return switch (category) {
            case "work" -> "工作";
            case "family" -> "家庭";
            case "personal" -> "个人";
            default -> "";
        } + "类";
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
