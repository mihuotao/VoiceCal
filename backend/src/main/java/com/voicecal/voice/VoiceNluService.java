package com.voicecal.voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VoiceNluService {

    private static final Logger log = LoggerFactory.getLogger(VoiceNluService.class);

    public NluResult parse(String text) {
        if (text == null || text.isBlank()) {
            NluResult result = new NluResult(text, "UNKNOWN", 0, "rule");
            result.setRequiresClarify(true);
            result.setClarifyQuestion("请告诉我您想做什么？");
            return result;
        }

        NluResult result = new NluResult(text, "UNKNOWN", 0.5, "rule");

        if (matchCreate(text, result)) return result;
        if (matchQuery(text, result)) return result;
        if (matchUpdate(text, result)) return result;
        if (matchDelete(text, result)) return result;
        if (matchReminder(text, result)) return result;

        result.setConfidence(0.3);
        result.setRequiresClarify(true);
        result.setClarifyQuestion("我没有理解您的意思，请重新说一遍。您可以试试说「下周三下午三点创建会议」");
        return result;
    }

    private boolean matchCreate(String text, NluResult result) {
        String[] patterns = {"创建", "新建", "添加", "安排", "新增", "加一个"};
        if (!containsAny(text, patterns)) return false;

        result.setIntent("CREATE");
        result.setConfidence(0.8);

        String title = extractTitle(text);
        result.addEntity("title", title);

        LocalDate date = extractDate(text);
        if (date != null) result.addEntity("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE));

        LocalTime startTime = extractTime(text);
        if (startTime != null) result.addEntity("startTime", startTime.format(DateTimeFormatter.ofPattern("HH:mm")));

        LocalTime endTime = extractEndTime(text, startTime);
        if (endTime != null) result.addEntity("endTime", endTime.format(DateTimeFormatter.ofPattern("HH:mm")));

        String category = extractCategory(text);
        if (category != null) result.addEntity("category", category);

        // Check if missing required fields
        if (!result.getEntities().containsKey("date")) {
            result.setRequiresClarify(true);
            result.setClarifyQuestion("请问在哪一天？");
            result.setMissingField("date");
            return true;
        }
        if (!result.getEntities().containsKey("startTime")) {
            result.setRequiresClarify(true);
            result.setClarifyQuestion("请问什么时间开始？");
            result.setMissingField("startTime");
            return true;
        }
        if (!result.getEntities().containsKey("endTime")) {
            // Set default 1 hour duration
            LocalTime start = LocalTime.parse(result.getEntities().get("startTime").toString());
            result.addEntity("endTime", start.plusHours(1).format(DateTimeFormatter.ofPattern("HH:mm")));
        }

        return true;
    }

    private boolean matchQuery(String text, NluResult result) {
        String[] patterns = {"查看", "查询", "显示", "有什么", "今天", "明天", "本周", "下周", "这个月"};
        if (!containsAny(text, patterns)) return false;

        result.setIntent("QUERY");
        result.setConfidence(0.7);

        LocalDate date = extractDate(text);
        if (date != null) {
            result.addEntity("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE));
        } else {
            result.addEntity("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        String category = extractCategory(text);
        if (category != null) result.addEntity("category", category);

        return true;
    }

    private boolean matchUpdate(String text, NluResult result) {
        String[] patterns = {"修改", "更改", "改一下", "重新安排", "改期", "推迟", "提前"};
        if (!containsAny(text, patterns)) return false;

        result.setIntent("UPDATE");
        result.setConfidence(0.7);

        String title = extractTitle(text);
        if (title != null) result.addEntity("title", title);

        LocalDate date = extractDate(text);
        if (date != null) result.addEntity("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE));

        // For update, we need at least the title to identify what to update
        if (title == null) {
            result.setRequiresClarify(true);
            result.setClarifyQuestion("请问要修改哪个事件？");
            result.setMissingField("title");
        }

        return true;
    }

    private boolean matchDelete(String text, NluResult result) {
        String[] patterns = {"删除", "取消", "移除", "去掉", "划掉"};
        if (!containsAny(text, patterns)) return false;

        result.setIntent("DELETE");
        result.setConfidence(0.7);

        String title = extractTitle(text);
        if (title != null) result.addEntity("title", title);

        LocalDate date = extractDate(text);
        if (date != null) result.addEntity("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE));

        if (title == null) {
            result.setRequiresClarify(true);
            result.setClarifyQuestion("请问要删除哪个事件？");
            result.setMissingField("title");
        }

        return true;
    }

    private boolean matchReminder(String text, NluResult result) {
        String[] patterns = {"提醒", "记得", "别忘", "通知", "设置"};
        if (!containsAny(text, patterns)) return false;

        result.setIntent("REMINDER");
        result.setConfidence(0.8);

        String title = extractTitle(text);
        result.addEntity("title", title != null ? title : "提醒事项");

        LocalDate date = extractDate(text);
        if (date != null) result.addEntity("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE));

        LocalTime time = extractTime(text);
        if (time != null) result.addEntity("time", time.format(DateTimeFormatter.ofPattern("HH:mm")));

        if (!result.getEntities().containsKey("date") || !result.getEntities().containsKey("time")) {
            result.setRequiresClarify(true);
            result.setClarifyQuestion("请问什么时间提醒您？");
            result.setMissingField("time");
        }

        return true;
    }

    private String extractTitle(String text) {
        String[] stopWords = {"创建", "新建", "添加", "安排", "删除", "取消", "修改", "更改",
                "查看", "查询", "提醒", "记得", "一个", "一下", "帮我", "我要", "我想", "给我",
                "下周三", "下周二", "下周一", "下周", "这周", "今天", "明天", "后天",
                "上午", "下午", "晚上", "点", "分", "半", "在", "到", "和", "的"};

        String cleaned = text;
        for (String word : stopWords) {
            cleaned = cleaned.replace(word, "");
        }

        cleaned = cleaned.replaceAll("[\\d\\s年月日时分：:]", "").trim();

        String[] commonCategories = {"会议", "约会", "聚餐", "吃饭", "开会", "面试", "培训",
                "出差", "旅行", "运动", "健身", "看医生", "上班", "下班", "生日",
                "纪念日", "聚会", "上课", "学习", "休息", "放假"};
        for (String cat : commonCategories) {
            if (cleaned.contains(cat)) {
                return cleaned.length() > 15 ? cleaned.substring(0, 15) : cleaned;
            }
        }
        if (cleaned.length() > 15) cleaned = cleaned.substring(0, 15);
        return cleaned.isEmpty() ? "新事件" : cleaned;
    }

    private LocalDate extractDate(String text) {
        LocalDate today = LocalDate.now();

        if (text.contains("今天") || text.contains("今日")) return today;
        if (text.contains("明天") || text.contains("明日")) return today.plusDays(1);
        if (text.contains("后天")) return today.plusDays(2);

        if (text.contains("下周一")) return today.with(java.time.DayOfWeek.MONDAY).plusWeeks(1);
        if (text.contains("下周二")) return today.with(java.time.DayOfWeek.TUESDAY).plusWeeks(1);
        if (text.contains("下周三")) return today.with(java.time.DayOfWeek.WEDNESDAY).plusWeeks(1);
        if (text.contains("下周四")) return today.with(java.time.DayOfWeek.THURSDAY).plusWeeks(1);
        if (text.contains("下周五")) return today.with(java.time.DayOfWeek.FRIDAY).plusWeeks(1);
        if (text.contains("下周六")) return today.with(java.time.DayOfWeek.SATURDAY).plusWeeks(1);
        if (text.contains("下周日")) return today.with(java.time.DayOfWeek.SUNDAY).plusWeeks(1);

        if (text.contains("下周末")) return today.with(java.time.DayOfWeek.SATURDAY).plusWeeks(1);
        if (text.contains("下周")) return today.with(java.time.DayOfWeek.MONDAY).plusWeeks(1);

        if (text.contains("周一") || text.contains("星期一")) return today.with(java.time.DayOfWeek.MONDAY);
        if (text.contains("周二") || text.contains("星期二")) return today.with(java.time.DayOfWeek.TUESDAY);
        if (text.contains("周三") || text.contains("星期三")) return today.with(java.time.DayOfWeek.WEDNESDAY);
        if (text.contains("周四") || text.contains("星期四")) return today.with(java.time.DayOfWeek.THURSDAY);
        if (text.contains("周五") || text.contains("星期五")) return today.with(java.time.DayOfWeek.FRIDAY);
        if (text.contains("周六") || text.contains("星期六")) return today.with(java.time.DayOfWeek.SATURDAY);
        if (text.contains("周日") || text.contains("星期日") || text.contains("周末")) {
            if (text.contains("本周末") || text.contains("这个周末")) {
                return today.with(java.time.DayOfWeek.SATURDAY);
            }
            return today.with(java.time.DayOfWeek.SUNDAY);
        }

        Pattern datePattern = Pattern.compile("(\\d{1,2})月(\\d{1,2})[号日]");
        Matcher m = datePattern.matcher(text);
        if (m.find()) {
            int month = Integer.parseInt(m.group(1));
            int day = Integer.parseInt(m.group(2));
            int year = month < today.getMonthValue() ? today.getYear() + 1 : today.getYear();
            return LocalDate.of(year, month, day);
        }

        return null;
    }

    private LocalTime extractTime(String text) {
        LocalTime now = LocalTime.now();

        if (text.contains("早上") || text.contains("早晨")) return LocalTime.of(8, 0);
        if (text.contains("上午")) return LocalTime.of(10, 0);
        if (text.contains("中午")) return LocalTime.of(12, 0);
        if (text.contains("下午")) return LocalTime.of(14, 0);
        if (text.contains("晚上")) return LocalTime.of(19, 0);
        if (text.contains("半夜") || text.contains("凌晨")) return LocalTime.of(0, 0);

        Pattern p1 = Pattern.compile("(\\d{1,2})[：:](\\d{2})");
        Matcher m1 = p1.matcher(text);
        if (m1.find()) return LocalTime.of(Integer.parseInt(m1.group(1)), Integer.parseInt(m1.group(2)));

        Pattern p2 = Pattern.compile("(\\d{1,2})点(\\d{1,2})分");
        Matcher m2 = p2.matcher(text);
        if (m2.find()) return LocalTime.of(Integer.parseInt(m2.group(1)), Integer.parseInt(m2.group(2)));

        Pattern p3 = Pattern.compile("(\\d{1,2})点(半)");
        Matcher m3 = p3.matcher(text);
        if (m3.find()) return LocalTime.of(Integer.parseInt(m3.group(1)), 30);

        Pattern p4 = Pattern.compile("(\\d{1,2})点");
        Matcher m4 = p4.matcher(text);
        if (m4.find()) return LocalTime.of(Integer.parseInt(m4.group(1)), 0);

        // Relative time
        if (text.contains("半小时后")) return now.plusMinutes(30).withSecond(0);
        if (text.contains("一小时后") || text.contains("1小时后")) return now.plusHours(1).withSecond(0);
        if (text.contains("两小时后") || text.contains("2小时后")) return now.plusHours(2).withSecond(0);

        return null;
    }

    private LocalTime extractEndTime(String text, LocalTime startTime) {
        Pattern p = Pattern.compile("到(\\d{1,2})点");
        Matcher m = p.matcher(text);
        if (m.find()) return LocalTime.of(Integer.parseInt(m.group(1)), 0);

        Pattern p2 = Pattern.compile("到(\\d{1,2})[：:](\\d{2})");
        Matcher m2 = p2.matcher(text);
        if (m2.find()) return LocalTime.of(Integer.parseInt(m2.group(1)), Integer.parseInt(m2.group(2)));

        if (text.contains("一个小时") || text.contains("一小时")) {
            return startTime != null ? startTime.plusHours(1) : LocalTime.now().plusHours(1);
        }
        if (text.contains("两个小时") || text.contains("两小时")) {
            return startTime != null ? startTime.plusHours(2) : LocalTime.now().plusHours(2);
        }

        return null;
    }

    private String extractCategory(String text) {
        if (text.contains("工作") || text.contains("会议") || text.contains("开会")
                || text.contains("面试") || text.contains("出差") || text.contains("培训")) return "work";
        if (text.contains("家庭") || text.contains("家人") || text.contains("家里")
                || text.contains("父母") || text.contains("孩子")) return "family";
        if (text.contains("个人") || text.contains("运动") || text.contains("健身")
                || text.contains("看医生") || text.contains("看病")) return "personal";
        return null;
    }

    private boolean containsAny(String text, String[] keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

}
