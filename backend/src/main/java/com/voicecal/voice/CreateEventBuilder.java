package com.voicecal.voice;

import com.voicecal.model.dto.CreateEventRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 语音创建事件构建器
 * 负责从 NLU 实体构建 CreateEventRequest，检查缺失字段，生成追问问题
 */
@Component
public class CreateEventBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 从 NLU 实体构建 CreateEventRequest
     */
    public CreateEventRequest build(NluResult nluResult) {
        Map<String, Object> entities = nluResult.getEntities();
        CreateEventRequest req = new CreateEventRequest();

        // 必填字段
        req.setTitle(getString(entities, "title", "新事件"));

        // 解析日期和时间
        LocalDate date = parseDate(getString(entities, "date", null));
        LocalTime startTime = parseTime(getString(entities, "startTime", null));
        LocalTime endTime = parseTime(getString(entities, "endTime", null));

        if (date == null) date = LocalDate.now();
        if (startTime == null) startTime = LocalTime.of(9, 0);
        if (endTime == null) endTime = startTime.plusHours(1);

        req.setStartTime(date.atTime(startTime));
        req.setEndTime(date.atTime(endTime));

        // 可选字段
        Object allDayObj = entities.get("allDay");
        if (allDayObj instanceof Boolean) {
            req.setAllDay((Boolean) allDayObj);
        } else {
            req.setAllDay(false);
        }

        req.setLocation(getString(entities, "location", null));
        req.setDescription(getString(entities, "description", null));
        req.setCategory(getString(entities, "category", "personal"));
        req.setColor(getString(entities, "color", getDefaultColor(req.getCategory())));

        Object priorityObj = entities.get("priority");
        if (priorityObj instanceof Number) {
            req.setPriority(((Number) priorityObj).intValue());
        } else {
            req.setPriority(0);
        }

        return req;
    }

    /**
     * 检查必填字段是否完整
     * @return 缺失的字段列表
     */
    public List<String> getMissingFields(NluResult nluResult) {
        Map<String, Object> entities = nluResult.getEntities();
        List<String> missing = new ArrayList<>();

        if (isBlank(entities.get("title"))) {
            missing.add("title");
        }
        if (isBlank(entities.get("date"))) {
            missing.add("date");
        }
        if (isBlank(entities.get("startTime"))) {
            missing.add("startTime");
        }

        return missing;
    }

    /**
     * 生成追问问题
     */
    public String generateClarifyQuestion(String missingField) {
        return switch (missingField) {
            case "title" -> "请问事件标题是什么？";
            case "date" -> "请问在哪一天？";
            case "startTime" -> "请问什么时间开始？";
            case "endTime" -> "请问什么时间结束？";
            case "location" -> "请问在哪里？";
            default -> "请补充" + missingField + "信息";
        };
    }

    /**
     * 构建预览数据（用于返回给前端展示）
     */
    public Map<String, Object> buildPreviewMap(NluResult nluResult) {
        CreateEventRequest req = build(nluResult);
        Map<String, Object> preview = new java.util.HashMap<>();
        preview.put("title", req.getTitle());
        preview.put("date", req.getStartTime().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        preview.put("startTime", req.getStartTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        preview.put("endTime", req.getEndTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        preview.put("allDay", req.getAllDay());
        preview.put("location", req.getLocation());
        preview.put("description", req.getDescription());
        preview.put("category", req.getCategory());
        preview.put("color", req.getColor());
        preview.put("priority", req.getPriority());
        return preview;
    }

    // ========== 私有辅助方法 ==========

    private String getString(Map<String, Object> entities, String key, String defaultValue) {
        Object val = entities.get(key);
        if (val == null) return defaultValue;
        String str = val.toString().trim();
        return str.isEmpty() || "null".equals(str) ? defaultValue : str;
    }

    private boolean isBlank(Object obj) {
        if (obj == null) return true;
        String str = obj.toString().trim();
        return str.isEmpty() || "null".equals(str);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr, DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return null;
        try {
            return LocalTime.parse(timeStr, TIME_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    private String getDefaultColor(String category) {
        if (category == null) return "#22c55e";
        return switch (category) {
            case "work" -> "#6366f1";
            case "health" -> "#f59e0b";
            case "social" -> "#ec4899";
            default -> "#22c55e";
        };
    }
}
