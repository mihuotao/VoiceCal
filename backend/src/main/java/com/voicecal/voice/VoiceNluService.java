package com.voicecal.voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VoiceNluService {

    private static final Logger log = LoggerFactory.getLogger(VoiceNluService.class);

    private final LlmNluService llmNluService;

    public VoiceNluService(LlmNluService llmNluService) {
        this.llmNluService = llmNluService;
    }

    // ==================== 意图关键词定义 ====================

    /** 查询意图指示词 — 高优先级，必须在 CREATE 之前检查 */
    private static final String[] QUERY_INDICATORS = {
            "查看", "查询", "显示", "有什么", "有哪些", "有没有",
            "看看", "查查", "翻翻", "找找", "搜搜",
            "日程", "安排表", "行程"
    };

    /** 创建意图关键词 */
    private static final String[] CREATE_KEYWORDS = {
            "创建", "新建", "添加", "新增", "加一个", "加个",
            "安排一个", "安排一下", "设一个",
            "开会", "会议", "培训", "出差", "面试", "约会",
            "聚餐", "聚会", "团建", "生日"
    };

    /** 独立日期关键词（用于 matchQuery 触发） */
    private static final String[] DATE_KEYWORDS = {
            "今天", "明天", "后天", "大后天",
            "昨天", "前天",
            "周一", "周二", "周三", "周四", "周五", "周六", "周日",
            "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日",
            "下周", "上周", "这周", "本周", "周末",
            "这个月", "上个月", "下个月", "月底"
    };

    // ==================== 汉字数字映射 ====================

    private static final java.util.Map<String, Integer> CHINESE_DIGITS;
    static {
        CHINESE_DIGITS = new java.util.HashMap<>();
        CHINESE_DIGITS.put("零", 0);
        CHINESE_DIGITS.put("一", 1); CHINESE_DIGITS.put("壹", 1);
        CHINESE_DIGITS.put("二", 2); CHINESE_DIGITS.put("两", 2); CHINESE_DIGITS.put("贰", 2);
        CHINESE_DIGITS.put("三", 3); CHINESE_DIGITS.put("叁", 3);
        CHINESE_DIGITS.put("四", 4); CHINESE_DIGITS.put("肆", 4);
        CHINESE_DIGITS.put("五", 5); CHINESE_DIGITS.put("伍", 5);
        CHINESE_DIGITS.put("六", 6); CHINESE_DIGITS.put("陆", 6);
        CHINESE_DIGITS.put("七", 7); CHINESE_DIGITS.put("柒", 7);
        CHINESE_DIGITS.put("八", 8); CHINESE_DIGITS.put("捌", 8);
        CHINESE_DIGITS.put("九", 9); CHINESE_DIGITS.put("玖", 9);
        CHINESE_DIGITS.put("十", 10); CHINESE_DIGITS.put("拾", 10);
        CHINESE_DIGITS.put("廿", 20);
        CHINESE_DIGITS.put("三十", 30); CHINESE_DIGITS.put("卅", 30);
    }

    // ==================== 入口 ====================

    public NluResult parse(String text) {
        if (text == null || text.isBlank()) {
            NluResult result = new NluResult(text, "UNKNOWN", 0, "rule");
            result.setRequiresClarify(true);
            result.setClarifyQuestion("请告诉我您想做什么？");
            return result;
        }

        // 第一步：优先调用 LLM（DeepSeek）解析
        NluResult llmResult = tryLlmParse(text);
        if (llmResult != null && llmResult.getConfidence() > 0.4) {
            log.info("NLU 结果来源: LLM, intent={}, confidence={}", llmResult.getIntent(), llmResult.getConfidence());
            return llmResult;
        }

        // 第二步：LLM 失败/超时/低置信度 → 关键字匹配
        NluResult keywordResult = parseWithKeywords(text);
        if (keywordResult != null) {
            log.info("NLU 结果来源: 关键字匹配, intent={}", keywordResult.getIntent());
            return keywordResult;
        }

        // 第三步：关键字匹配失败 → 规则引擎兜底
        log.info("NLU 降级到规则引擎");
        return parseWithRules(text);
    }

    /**
     * 关键字优先匹配（最高优先级）
     * 包含关键字直接返回对应意图，不调用 LLM
     */
    private NluResult parseWithKeywords(String text) {
        // 创建关键字
        if (containsAny(text, CREATE_KEYWORDS)) {
            NluResult result = new NluResult(text, "CREATE", 0.95, "keyword");
            extractCreateEntities(text, result);
            return result;
        }

        // 查询关键字
        if (containsAny(text, QUERY_INDICATORS)) {
            NluResult result = new NluResult(text, "QUERY", 0.95, "keyword");
            extractQueryEntities(text, result);
            return result;
        }

        // 修改关键字
        String[] UPDATE_KEYWORDS = {"修改", "更改", "编辑", "改一下"};
        if (containsAny(text, UPDATE_KEYWORDS)) {
            NluResult result = new NluResult(text, "UPDATE", 0.95, "keyword");
            extractUpdateEntities(text, result);
            return result;
        }

        // 删除关键字
        String[] DELETE_KEYWORDS = {"删除", "取消", "移除", "去掉"};
        if (containsAny(text, DELETE_KEYWORDS)) {
            NluResult result = new NluResult(text, "DELETE", 0.95, "keyword");
            extractDeleteEntities(text, result);
            return result;
        }

        // 提醒关键字
        String[] REMINDER_KEYWORDS = {"提醒", "记得", "别忘"};
        if (containsAny(text, REMINDER_KEYWORDS)) {
            NluResult result = new NluResult(text, "REMINDER", 0.95, "keyword");
            extractReminderEntities(text, result);
            return result;
        }

        // 没有匹配到关键字
        return null;
    }

    /**
     * 提取创建事件的实体
     */
    private void extractCreateEntities(String text, NluResult result) {
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

        String location = extractLocation(text);
        if (location != null) result.addEntity("location", location);

        // 如果没有日期，默认明天
        if (!result.getEntities().containsKey("date")) {
            result.addEntity("date", LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        // 如果没有开始时间，默认 09:00
        if (!result.getEntities().containsKey("startTime")) {
            result.addEntity("startTime", "09:00");
        }

        // 如果没有结束时间，默认开始时间 + 1 小时
        if (!result.getEntities().containsKey("endTime")) {
            String startStr = (String) result.getEntities().get("startTime");
            LocalTime start = LocalTime.parse(startStr);
            result.addEntity("endTime", start.plusHours(1).format(DateTimeFormatter.ofPattern("HH:mm")));
        }
    }

    /**
     * 提取查询事件的实体
     */
    private void extractQueryEntities(String text, NluResult result) {
        LocalDate[] range = extractDateRange(text);
        result.addEntity("startDate", range[0].format(DateTimeFormatter.ISO_LOCAL_DATE));
        result.addEntity("endDate", range[1].format(DateTimeFormatter.ISO_LOCAL_DATE));

        if (range[0].equals(range[1])) {
            result.addEntity("date", range[0].format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        String category = extractCategory(text);
        if (category != null) result.addEntity("category", category);
    }

    /**
     * 提取修改事件的实体
     */
    private void extractUpdateEntities(String text, NluResult result) {
        String title = extractTitle(text);
        if (title != null) result.addEntity("title", title);

        LocalDate date = extractDate(text);
        if (date != null) result.addEntity("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE));

        LocalTime newTime = extractTime(text);
        if (newTime != null) result.addEntity("newStartTime", newTime.format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    /**
     * 提取删除事件的实体
     */
    private void extractDeleteEntities(String text, NluResult result) {
        String title = extractTitle(text);
        if (title != null) result.addEntity("title", title);

        LocalDate date = extractDate(text);
        if (date != null) result.addEntity("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    /**
     * 提取提醒事件的实体
     */
    private void extractReminderEntities(String text, NluResult result) {
        String title = extractTitle(text);
        result.addEntity("title", title != null ? title : "提醒事项");

        LocalDate date = extractDate(text);
        if (date != null) result.addEntity("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE));

        LocalTime time = extractTime(text);
        if (time != null) result.addEntity("time", time.format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    /**
     * 规则引擎解析（LLM 失败时的降级方案）
     */
    private NluResult parseWithRules(String text) {
        NluResult result = new NluResult(text, "UNKNOWN", 0.5, "rule");

        if (matchQuery(text, result)) return result;
        if (matchCreate(text, result)) return result;
        if (matchUpdate(text, result)) return result;
        if (matchDelete(text, result)) return result;
        if (matchReminder(text, result)) return result;

        result.setConfidence(0.3);
        result.setRequiresClarify(true);
        result.setClarifyQuestion("我没有理解您的意思，请重新说一遍。您可以试试说「下周三下午三点创建会议」");
        return result;
    }

    /**
     * 调用 LLM 解析，异常时安全返回 null
     */
    private NluResult tryLlmParse(String text) {
        try {
            return llmNluService.parse(text);
        } catch (Exception e) {
            log.warn("LLM NLU 调用失败，降级到规则引擎", e);
            return null;
        }
    }

    // ==================== 意图匹配 ====================

    private boolean matchQuery(String text, NluResult result) {
        // 判断是否为查询意图
        boolean hasQueryIndicator = containsAny(text, QUERY_INDICATORS);
        boolean hasDateKeyword = containsAny(text, DATE_KEYWORDS);

        // "安排" 只在有查询上下文时才算查询（"有什么安排" vs "安排一个会议"）
        boolean hasArrangementQuery = text.contains("安排") && hasQueryIndicator;

        if (!hasQueryIndicator && !hasDateKeyword && !hasArrangementQuery) return false;

        result.setIntent("QUERY");
        result.setConfidence(0.8);

        // 提取日期范围
        LocalDate[] range = extractDateRange(text);
        result.addEntity("startDate", range[0].format(DateTimeFormatter.ISO_LOCAL_DATE));
        result.addEntity("endDate", range[1].format(DateTimeFormatter.ISO_LOCAL_DATE));

        // 兼容旧逻辑：单日查询也设置 date 字段
        if (range[0].equals(range[1])) {
            result.addEntity("date", range[0].format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        // 提取分类
        String category = extractCategory(text);
        if (category != null) result.addEntity("category", category);

        return true;
    }

    private boolean matchCreate(String text, NluResult result) {
        if (!containsAny(text, CREATE_KEYWORDS)) return false;

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

        String location = extractLocation(text);
        if (location != null) result.addEntity("location", location);

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
            LocalTime start = LocalTime.parse(result.getEntities().get("startTime").toString());
            result.addEntity("endTime", start.plusHours(1).format(DateTimeFormatter.ofPattern("HH:mm")));
        }

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

        // 提取新时间（如"改到下午3点"）
        LocalTime newTime = extractTime(text);
        if (newTime != null) result.addEntity("newStartTime", newTime.format(DateTimeFormatter.ofPattern("HH:mm")));

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
        String[] patterns = {"提醒", "记得", "别忘", "通知"};
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

    // ==================== 日期范围提取（核心增强） ====================

    /**
     * 提取日期范围，支持"本周"、"这个月"、"下周末"等范围表达
     * @return LocalDate[2]，[0]=startDate, [1]=endDate
     */
    private LocalDate[] extractDateRange(String text) {
        LocalDate today = LocalDate.now();

        // === 范围表达 ===
        if (text.contains("本周") || text.contains("这周")) {
            LocalDate start = today.with(DayOfWeek.MONDAY);
            LocalDate end = today.with(DayOfWeek.SUNDAY);
            return new LocalDate[]{start, end};
        }
        if (text.contains("这个月")) {
            LocalDate start = today.withDayOfMonth(1);
            LocalDate end = today.withDayOfMonth(today.lengthOfMonth());
            return new LocalDate[]{start, end};
        }
        if (text.contains("上个月")) {
            LocalDate lastMonth = today.minusMonths(1);
            LocalDate start = lastMonth.withDayOfMonth(1);
            LocalDate end = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());
            return new LocalDate[]{start, end};
        }
        if (text.contains("下个月")) {
            LocalDate nextMonth = today.plusMonths(1);
            LocalDate start = nextMonth.withDayOfMonth(1);
            LocalDate end = nextMonth.withDayOfMonth(nextMonth.lengthOfMonth());
            return new LocalDate[]{start, end};
        }
        if (text.contains("下周末")) {
            LocalDate sat = today.with(DayOfWeek.SATURDAY).plusWeeks(1);
            LocalDate sun = today.with(DayOfWeek.SUNDAY).plusWeeks(1);
            return new LocalDate[]{sat, sun};
        }
        if (text.contains("本周末") || text.contains("这个周末")) {
            LocalDate sat = today.with(DayOfWeek.SATURDAY);
            LocalDate sun = today.with(DayOfWeek.SUNDAY);
            return new LocalDate[]{sat, sun};
        }
        if (text.contains("最近三天")) {
            return new LocalDate[]{today, today.plusDays(2)};
        }
        if (text.contains("最近一周") || text.contains("最近一个礼拜")) {
            return new LocalDate[]{today.minusDays(6), today};
        }
        if (text.contains("月底")) {
            return new LocalDate[]{today, today.withDayOfMonth(today.lengthOfMonth())};
        }

        // === 单日表达 ===
        LocalDate single = extractDate(text);
        if (single != null) {
            return new LocalDate[]{single, single};
        }

        // 默认今天
        return new LocalDate[]{today, today};
    }

    // ==================== 单日日期提取（核心增强） ====================

    /**
     * 从中文文本中提取日期，支持：
     * - 相对日期：今天、明天、后天、大后天、大大后天
     * - X天后/前天/昨天
     * - 下周X、上周X、这周X
     * - X月Y号/日（数字和汉字）
     * - 单独的X号（默认当月）
     * - 汉字数字：五月三十一号
     */
    private LocalDate extractDate(String text) {
        LocalDate today = LocalDate.now();

        // === 相对天数 ===
        if (text.contains("今天") || text.contains("今日")) return today;
        if (text.contains("明天") || text.contains("明日")) return today.plusDays(1);
        if (text.contains("大后天")) return today.plusDays(3);
        if (text.contains("后天")) return today.plusDays(2);
        if (text.contains("昨天")) return today.minusDays(1);
        if (text.contains("前天")) return today.minusDays(2);

        // "X天后" 模式（数字）
        Pattern daysLaterNum = Pattern.compile("(\\d{1,2})天后");
        Matcher mDaysLaterNum = daysLaterNum.matcher(text);
        if (mDaysLaterNum.find()) {
            return today.plusDays(Integer.parseInt(mDaysLaterNum.group(1)));
        }

        // "X天后" 模式（汉字）
        Pattern daysLaterCN = Pattern.compile("([一二两三四五六七八九十]+)天后");
        Matcher mDaysLaterCN = daysLaterCN.matcher(text);
        if (mDaysLaterCN.find()) {
            return today.plusDays(parseChineseNumber(mDaysLaterCN.group(1)));
        }

        // === 下周X（精确匹配，避免"下周"误匹配） ===
        if (text.contains("下周一")) return safeFutureDate(today, DayOfWeek.MONDAY, 1);
        if (text.contains("下周二")) return safeFutureDate(today, DayOfWeek.TUESDAY, 1);
        if (text.contains("下周三")) return safeFutureDate(today, DayOfWeek.WEDNESDAY, 1);
        if (text.contains("下周四")) return safeFutureDate(today, DayOfWeek.THURSDAY, 1);
        if (text.contains("下周五")) return safeFutureDate(today, DayOfWeek.FRIDAY, 1);
        if (text.contains("下周六")) return safeFutureDate(today, DayOfWeek.SATURDAY, 1);
        if (text.contains("下周日") || text.contains("下周天")) return safeFutureDate(today, DayOfWeek.SUNDAY, 1);
        if (text.contains("下周")) return today.with(DayOfWeek.MONDAY).plusWeeks(1);

        // === 上周X ===
        if (text.contains("上周一")) return today.with(DayOfWeek.MONDAY).minusWeeks(1);
        if (text.contains("上周二")) return today.with(DayOfWeek.TUESDAY).minusWeeks(1);
        if (text.contains("上周三")) return today.with(DayOfWeek.WEDNESDAY).minusWeeks(1);
        if (text.contains("上周四")) return today.with(DayOfWeek.THURSDAY).minusWeeks(1);
        if (text.contains("上周五")) return today.with(DayOfWeek.FRIDAY).minusWeeks(1);
        if (text.contains("上周六")) return today.with(DayOfWeek.SATURDAY).minusWeeks(1);
        if (text.contains("上周日") || text.contains("上周天")) return today.with(DayOfWeek.SUNDAY).minusWeeks(1);
        if (text.contains("上周")) return today.with(DayOfWeek.MONDAY).minusWeeks(1);

        // === 这周X / 本周X ===
        if (text.contains("这周一") || text.contains("本周一")) return resolveWeekdayThisWeek(today, DayOfWeek.MONDAY);
        if (text.contains("这周二") || text.contains("本周二")) return resolveWeekdayThisWeek(today, DayOfWeek.TUESDAY);
        if (text.contains("这周三") || text.contains("本周三")) return resolveWeekdayThisWeek(today, DayOfWeek.WEDNESDAY);
        if (text.contains("这周四") || text.contains("本周四")) return resolveWeekdayThisWeek(today, DayOfWeek.THURSDAY);
        if (text.contains("这周五") || text.contains("本周五")) return resolveWeekdayThisWeek(today, DayOfWeek.FRIDAY);
        if (text.contains("这周六") || text.contains("本周六")) return resolveWeekdayThisWeek(today, DayOfWeek.SATURDAY);
        if (text.contains("这周日") || text.contains("本周日") || text.contains("这周天")) {
            return resolveWeekdayThisWeek(today, DayOfWeek.SUNDAY);
        }

        // === X月Y号/日（阿拉伯数字） ===
        Pattern datePattern1 = Pattern.compile("(\\d{1,2})月(\\d{1,2})[号日]");
        Matcher m1 = datePattern1.matcher(text);
        if (m1.find()) {
            int month = Integer.parseInt(m1.group(1));
            int day = Integer.parseInt(m1.group(2));
            return safeDate(today, month, day);
        }

        // === X月Y号/日（汉字数字：五月三十一号） ===
        Pattern datePatternCN = Pattern.compile("([一二三四五六七八九十]+)月([一二三四五六七八九十廿卅]+)[号日]");
        Matcher mCN = datePatternCN.matcher(text);
        if (mCN.find()) {
            int month = parseChineseNumber(mCN.group(1));
            int day = parseChineseNumber(mCN.group(2));
            if (month > 0 && month <= 12 && day > 0 && day <= 31) {
                return safeDate(today, month, day);
            }
        }

        // === X月Y号（混合：5月三十一号） ===
        Pattern datePatternMix1 = Pattern.compile("(\\d{1,2})月([一二三四五六七八九十廿卅]+)[号日]");
        Matcher mMix1 = datePatternMix1.matcher(text);
        if (mMix1.find()) {
            int month = Integer.parseInt(mMix1.group(1));
            int day = parseChineseNumber(mMix1.group(2));
            if (month > 0 && month <= 12 && day > 0 && day <= 31) {
                return safeDate(today, month, day);
            }
        }

        // === X月Y号（混合：五月31号） ===
        Pattern datePatternMix2 = Pattern.compile("([一二三四五六七八九十]+)月(\\d{1,2})[号日]");
        Matcher mMix2 = datePatternMix2.matcher(text);
        if (mMix2.find()) {
            int month = parseChineseNumber(mMix2.group(1));
            int day = Integer.parseInt(mMix2.group(2));
            if (month > 0 && month <= 12 && day > 0 && day <= 31) {
                return safeDate(today, month, day);
            }
        }

        // === 单独的X号/日（无月份，默认当月） ===
        Pattern dayOnlyNum = Pattern.compile("(?:^|[\\s,，。])(\\d{1,2})[号日]");
        Matcher mDayNum = dayOnlyNum.matcher(text);
        if (mDayNum.find()) {
            int day = Integer.parseInt(mDayNum.group(1));
            if (day >= 1 && day <= 31) {
                LocalDate result = today.withDayOfMonth(Math.min(day, today.lengthOfMonth()));
                // 如果结果早于今天5天以上，可能是下个月
                if (result.isBefore(today.minusDays(5))) {
                    result = result.plusMonths(1);
                }
                return result;
            }
        }

        // === 单独的汉字数字号（"三十一号"） ===
        Pattern dayOnlyCN = Pattern.compile("([一二三四五六七八九十廿卅]+)[号日]");
        Matcher mDayCN = dayOnlyCN.matcher(text);
        if (mDayCN.find()) {
            int day = parseChineseNumber(mDayCN.group(1));
            if (day >= 1 && day <= 31) {
                LocalDate result = today.withDayOfMonth(Math.min(day, today.lengthOfMonth()));
                if (result.isBefore(today.minusDays(5))) {
                    result = result.plusMonths(1);
                }
                return result;
            }
        }

        // === X月（无日，默认当月1号 — 用于"五月有什么安排"） ===
        Pattern monthOnly = Pattern.compile("(\\d{1,2}|[一二三四五六七八九十]+)月(?!\\d|[一二三四五六七八九十])");
        Matcher mMonth = monthOnly.matcher(text);
        if (mMonth.find()) {
            String monthStr = mMonth.group(1);
            int month;
            try {
                month = Integer.parseInt(monthStr);
            } catch (NumberFormatException e) {
                month = parseChineseNumber(monthStr);
            }
            if (month >= 1 && month <= 12) {
                int year = month < today.getMonthValue() ? today.getYear() + 1 : today.getYear();
                LocalDate start = LocalDate.of(year, month, 1);
                return start; // 返回月初，调用方需配合范围查询
            }
        }

        return null;
    }

    // ==================== 汉字数字解析 ====================

    /**
     * 解析汉字数字为阿拉伯数字
     * 支持：一~三十、二十一、十五、廿五 等
     */
    private int parseChineseNumber(String cn) {
        if (cn == null || cn.isEmpty()) return 0;

        // 先尝试直接查表
        Integer direct = CHINESE_DIGITS.get(cn);
        if (direct != null) return direct;

        // 处理 "二十一" 这类组合
        int result = 0;
        int current = 0;

        for (int i = 0; i < cn.length(); i++) {
            String ch = cn.substring(i, i + 1);
            Integer val = CHINESE_DIGITS.get(ch);
            if (val == null) {
                // 尝试双字符匹配（"三十"）
                if (i + 1 < cn.length()) {
                    String twoChars = cn.substring(i, i + 2);
                    val = CHINESE_DIGITS.get(twoChars);
                    if (val != null) {
                        result += val;
                        i++; // 跳过下一个字符
                        continue;
                    }
                }
                continue;
            }

            if (val == 10) {
                // "十" 前面没有数字时当作 10
                result += (current == 0 ? 1 : current) * 10;
                current = 0;
            } else if (val == 20 || val == 30) {
                result += val;
                current = 0;
            } else {
                current = val;
            }
        }
        result += current;

        return result > 0 ? result : 0;
    }

    // ==================== 日期辅助方法 ====================

    /**
     * 安全的周几计算：确保返回的日期在今天之后（用于"下周X"）
     */
    private LocalDate safeFutureDate(LocalDate today, DayOfWeek targetDay, int weeksAhead) {
        LocalDate target = today.with(targetDay).plusWeeks(weeksAhead);
        // 确保在今天之后
        while (!target.isAfter(today)) {
            target = target.plusWeeks(1);
        }
        return target;
    }

    /**
     * 本周内的周几（如果已过则返回今天之后的最近一天）
     */
    private LocalDate resolveWeekdayThisWeek(LocalDate today, DayOfWeek targetDay) {
        LocalDate target = today.with(targetDay);
        // 如果已过，返回下周的这一天
        if (target.isBefore(today)) {
            target = target.plusWeeks(1);
        }
        return target;
    }

    /**
     * 安全的日期构造：处理月份和日期的边界
     */
    private LocalDate safeDate(LocalDate today, int month, int day) {
        int year = month < today.getMonthValue() ? today.getYear() + 1 : today.getYear();
        try {
            return LocalDate.of(year, month, day);
        } catch (java.time.DateTimeException e) {
            // 日期无效（如2月30日），返回该月最后一天
            return YearMonth.of(year, month).atEndOfMonth();
        }
    }

    // ==================== 标题提取 ====================

    private String extractTitle(String text) {
        String[] stopWords = {"创建", "新建", "添加", "安排", "删除", "取消", "修改", "更改",
                "查看", "查询", "提醒", "记得", "一个", "一下", "帮我", "我要", "我想", "给我",
                "下周一", "下周二", "下周三", "下周四", "下周五", "下周六", "下周日", "下周",
                "这周", "本周", "今天", "明天", "后天", "大后天", "昨天", "前天",
                "上午", "下午", "晚上", "点", "分", "半", "在", "到", "和", "的",
                "月底", "上个月", "下个月", "这个月", "周末"};

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
        return cleaned.isEmpty() ? null : cleaned;
    }

    // ==================== 时间提取 ====================

    private LocalTime extractTime(String text) {
        if (text.contains("早上") || text.contains("早晨")) return LocalTime.of(8, 0);
        if (text.contains("上午")) return LocalTime.of(10, 0);
        if (text.contains("中午")) return LocalTime.of(12, 0);
        if (text.contains("下午")) return LocalTime.of(14, 0);
        if (text.contains("晚上")) return LocalTime.of(19, 0);
        if (text.contains("半夜") || text.contains("凌晨")) return LocalTime.of(0, 0);

        // 数字格式：14:30
        Pattern p1 = Pattern.compile("(\\d{1,2})[：:](\\d{2})");
        Matcher m1 = p1.matcher(text);
        if (m1.find()) return LocalTime.of(Integer.parseInt(m1.group(1)), Integer.parseInt(m1.group(2)));

        // 汉字格式：三点二十分
        Pattern pCN = Pattern.compile("([一二三四五六七八九十]+)点([一二三四五六七八九十]+)分");
        Matcher mCN = pCN.matcher(text);
        if (mCN.find()) {
            int hour = parseChineseNumber(mCN.group(1));
            int minute = parseChineseNumber(mCN.group(2));
            if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
                return LocalTime.of(hour, minute);
            }
        }

        // 三点半
        Pattern pCNHalf = Pattern.compile("([一二三四五六七八九十]+)点半");
        Matcher mCNHalf = pCNHalf.matcher(text);
        if (mCNHalf.find()) {
            int hour = parseChineseNumber(mCNHalf.group(1));
            if (hour >= 0 && hour <= 23) return LocalTime.of(hour, 30);
        }

        // 三点
        Pattern pCNHour = Pattern.compile("([一二三四五六七八九十]+)点");
        Matcher mCNHour = pCNHour.matcher(text);
        if (mCNHour.find()) {
            int hour = parseChineseNumber(mCNHour.group(1));
            if (hour >= 0 && hour <= 23) return LocalTime.of(hour, 0);
        }

        // 数字格式：3点20分
        Pattern p2 = Pattern.compile("(\\d{1,2})点(\\d{1,2})分");
        Matcher m2 = p2.matcher(text);
        if (m2.find()) return LocalTime.of(Integer.parseInt(m2.group(1)), Integer.parseInt(m2.group(2)));

        // 3点半
        Pattern p3 = Pattern.compile("(\\d{1,2})点(半)");
        Matcher m3 = p3.matcher(text);
        if (m3.find()) return LocalTime.of(Integer.parseInt(m3.group(1)), 30);

        // 3点
        Pattern p4 = Pattern.compile("(\\d{1,2})点");
        Matcher m4 = p4.matcher(text);
        if (m4.find()) return LocalTime.of(Integer.parseInt(m4.group(1)), 0);

        // 相对时间
        if (text.contains("半小时后")) return LocalTime.now().plusMinutes(30).withSecond(0);
        if (text.contains("一小时后") || text.contains("1小时后")) return LocalTime.now().plusHours(1).withSecond(0);
        if (text.contains("两小时后") || text.contains("2小时后")) return LocalTime.now().plusHours(2).withSecond(0);

        return null;
    }

    // ==================== 结束时间提取 ====================

    private LocalTime extractEndTime(String text, LocalTime startTime) {
        Pattern p = Pattern.compile("到(\\d{1,2})点");
        Matcher m = p.matcher(text);
        if (m.find()) return LocalTime.of(Integer.parseInt(m.group(1)), 0);

        Pattern p2 = Pattern.compile("到(\\d{1,2})[：:](\\d{2})");
        Matcher m2 = p2.matcher(text);
        if (m2.find()) return LocalTime.of(Integer.parseInt(m2.group(1)), Integer.parseInt(m2.group(2)));

        // 汉字：到X点
        Pattern pCN = Pattern.compile("到([一二三四五六七八九十]+)点");
        Matcher mCN = pCN.matcher(text);
        if (mCN.find()) {
            int hour = parseChineseNumber(mCN.group(1));
            if (hour >= 0 && hour <= 23) return LocalTime.of(hour, 0);
        }

        if (text.contains("一个小时") || text.contains("一小时")) {
            return startTime != null ? startTime.plusHours(1) : LocalTime.now().plusHours(1);
        }
        if (text.contains("两个小时") || text.contains("两小时")) {
            return startTime != null ? startTime.plusHours(2) : LocalTime.now().plusHours(2);
        }

        return null;
    }

    // ==================== 分类提取 ====================

    private String extractCategory(String text) {
        if (text.contains("工作") || text.contains("会议") || text.contains("开会")
                || text.contains("面试") || text.contains("出差") || text.contains("培训")) return "work";
        if (text.contains("家庭") || text.contains("家人") || text.contains("家里")
                || text.contains("父母") || text.contains("孩子")) return "family";
        if (text.contains("个人") || text.contains("运动") || text.contains("健身")
                || text.contains("看医生") || text.contains("看病")) return "personal";
        return null;
    }

    // ==================== 地点提取 ====================

    /**
     * 从文本中提取地点信息
     * 支持："在XXX"、"于XXX"、"到XXX"
     */
    private String extractLocation(String text) {
        // 匹配 "在XXX" 模式
        Pattern p1 = Pattern.compile("在([^，。,.\\s]+(?:室|厅|楼|馆|场|园|区|中心|会议室|办公室|咖啡厅|餐厅)?)");
        Matcher m1 = p1.matcher(text);
        if (m1.find()) {
            String location = m1.group(1);
            if (location.length() >= 2 && location.length() <= 20) {
                return location;
            }
        }

        // 匹配 "于XXX" 模式
        Pattern p2 = Pattern.compile("于([^，。,.\\s]+(?:室|厅|楼|馆|场|园|区|中心|会议室|办公室)?)");
        Matcher m2 = p2.matcher(text);
        if (m2.find()) {
            String location = m2.group(1);
            if (location.length() >= 2 && location.length() <= 20) {
                return location;
            }
        }

        return null;
    }

    // ==================== 通用工具 ====================

    private boolean containsAny(String text, String[] keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

}
