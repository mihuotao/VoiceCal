# VoiceCal v1 - 项目架构文档

## 智能语音日历工具架构设计

---

## 📋 一、文档概述

| 项目   | 说明                                 |
| ---- | ---------------------------------- |
| 文档版本 | v1.0                               |
| 基于版本 | 初始版_mimo                           |
| 创建日期 | 2026-05-29                         |
| 新增特性 | Redis 缓存、Docker 部署、冲突检测、智能排期、百度云语音 |

### v1 相比初始版的改进

```
初始版_mimo                          v1 版本
    │                                   │
    ├── 基础语音交互                     ├── 基础语音交互
    ├── MySQL 存储                      ├── MySQL 存储
    ├── WebSocket 通信                  ├── WebSocket 通信
    │                                   ├── ✨ Redis 缓存加速
    │                                   ├── ✨ Docker 容器化部署
    │                                   ├── ✨ 智能冲突检测
    │                                   ├── ✨ 智能排期引擎
    │                                   └── ✨ 百度云语音集成
```

---

## 🏗️ 二、系统整体架构

### 2.1 架构总览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Docker Compose 编排层                             │
│                                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   Nginx     │  │  Frontend   │  │   Backend   │  │   MySQL     │        │
│  │  (反向代理)  │  │  (Vue 3)    │  │ (Spring Boot)│  │  (数据库)   │        │
│  │  :80/:443   │  │   :5173     │  │   :8080     │  │   :3306     │        │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │
│                                                                             │
│  ┌─────────────┐  ┌─────────────┐                                          │
│  │    Redis    │  │  WebSocket  │                                          │
│  │   (缓存)    │  │   (实时)    │                                          │
│  │   :6379     │  │   :8081     │                                          │
│  └─────────────┘  └─────────────┘                                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ HTTPS/WSS
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              用户终端 (浏览器)                               │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                         Vue 3 前端应用                                 │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │  │
│  │  │  语音模块   │  │  日历模块   │  │  缓存模块   │  │  通知模块   │  │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ API 调用
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           百度云语音服务                                     │
│                                                                             │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐  │
│  │  语音识别 (ASR)     │  │  语音合成 (TTS)     │  │  NLU 自然语言理解   │  │
│  │  流式实时识别       │  │  多音色语音合成     │  │  意图/实体/时间     │  │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 分层架构设计

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              接入层 (Ingress)                               │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │  Nginx: 反向代理、负载均衡、SSL 终止、静态资源服务                       │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            应用层 (Application)                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │  Spring Boot 3.2 单体应用                                             │  │
│  │                                                                       │  │
│  │  ┌─────────────────────────────────────────────────────────────────┐  │  │
│  │  │  Controller 层                                                  │  │  │
│  │  │  ├── CalendarController    日历事件 API                         │  │  │
│  │  │  ├── VoiceController       语音相关 API                         │  │  │
│  │  │  ├── ScheduleController    智能排期 API                         │  │  │
│  │  │  └── UserController        用户管理 API                         │  │  │
│  │  └─────────────────────────────────────────────────────────────────┘  │  │
│  │                                                                       │  │
│  │  ┌─────────────────────────────────────────────────────────────────┐  │  │
│  │  │  Service 层                                                     │  │  │
│  │  │  ├── CalendarService       日历核心服务                         │  │  │
│  │  │  ├── VoiceService          语音处理服务                         │  │  │
│  │  │  ├── NLUService            自然语言理解服务                      │  │  │
│  │  │  ├── ConflictService       冲突检测服务                         │  │  │
│  │  │  ├── ScheduleService       智能排期服务                         │  │  │
│  │  │  ├── ReminderService       提醒服务                            │  │  │
│  │  │  └── CacheService          缓存服务                            │  │  │
│  │  └─────────────────────────────────────────────────────────────────┘  │  │
│  │                                                                       │  │
│  │  ┌─────────────────────────────────────────────────────────────────┐  │  │
│  │  │  WebSocket 层                                                   │  │  │
│  │  │  └── VoiceWebSocketHandler  实时语音流处理                       │  │  │
│  │  └─────────────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                             数据层 (Data)                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │    MySQL    │  │    Redis    │  │  百度云 API  │  │  本地文件   │        │
│  │   持久化    │  │    缓存     │  │   语音服务   │  │    存储     │        │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔧 三、核心模块设计

### 3.1 Redis 缓存模块

#### 3.1.1 缓存策略设计

```
缓存层次结构：
┌─────────────────────────────────────────────────────────────────┐
│                        Redis 缓存集群                           │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  L1: 热点数据缓存 (TTL: 5分钟)                            │  │
│  │  ├── calendar:today:{userId}        今日日程              │  │
│  │  ├── calendar:week:{userId}         本周日程              │  │
│  │  └── voice:session:{sessionId}      语音会话状态          │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  L2: 业务数据缓存 (TTL: 30分钟)                           │  │
│  │  ├── calendar:month:{userId}:{month}  月度日程            │  │
│  │  ├── user:profile:{userId}           用户信息             │  │
│  │  └── conflict:check:{userId}:{date}  冲突检测结果         │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  L3: 持久化缓存 (TTL: 24小时)                             │  │
│  │  ├── user:preferences:{userId}      用户偏好设置          │  │
│  │  └── nlu:history:{userId}           NLU 历史记录          │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 3.1.2 缓存 Key 设计规范

```java
public class CacheKeyBuilder {

    // 日历缓存 Key
    public static String calendarToday(Long userId) {
        return String.format("calendar:today:%d", userId);
    }

    public static String calendarWeek(Long userId, String weekStart) {
        return String.format("calendar:week:%d:%s", userId, weekStart);
    }

    public static String calendarMonth(Long userId, String month) {
        return String.format("calendar:month:%d:%s", userId, month);
    }

    // 冲突检测缓存 Key
    public static String conflictCheck(Long userId, String date) {
        return String.format("conflict:check:%d:%s", userId, date);
    }

    // 语音会话缓存 Key
    public static String voiceSession(String sessionId) {
        return String.format("voice:session:%s", sessionId);
    }

    // 智能排期缓存 Key
    public static String scheduleSuggestion(Long userId, String dateRange) {
        return String.format("schedule:suggest:%d:%s", userId, dateRange);
    }
}
```

#### 3.1.3 缓存更新策略

```
写入策略：Cache-Aside (旁路缓存)
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│   写入流程：                                                 │
│   1. 更新 MySQL 数据库                                       │
│   2. 删除相关缓存 Key                                        │
│   3. 下次读取时重新加载缓存                                   │
│                                                              │
│   读取流程：                                                 │
│   1. 先查 Redis 缓存                                        │
│   2. 缓存命中 → 直接返回                                    │
│   3. 缓存未命中 → 查 MySQL → 写入缓存 → 返回                │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

#### 3.1.4 CacheService 实现

```java
@Service
@Slf4j
public class CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取缓存，支持自动加载
     */
    public <T> T getOrLoad(String key, Duration ttl, Supplier<T> loader) {
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.debug("Cache hit: {}", key);
            return (T) cached;
        }

        log.debug("Cache miss: {}", key);
        T value = loader.get();
        if (value != null) {
            redisTemplate.opsForValue().set(key, value, ttl);
        }
        return value;
    }

    /**
     * 批量删除匹配模式的缓存
     */
    public void evictPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Evicted {} keys matching pattern: {}", keys.size(), pattern);
        }
    }

    /**
     * 用户日程缓存失效
     */
    public void evictUserCalendarCache(Long userId) {
        evictPattern("calendar:*:" + userId + "*");
        evictPattern("conflict:*:" + userId + "*");
        evictPattern("schedule:*:" + userId + "*");
    }
}
```

### 3.2 智能冲突检测模块

#### 3.2.1 冲突检测算法

```
冲突检测流程：
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  输入：新事件 (startTime, endTime, location)                    │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Step 1: 时间冲突检测                                     │  │
│  │  查询条件：                                                │  │
│  │  - 同一用户                                               │  │
│  │  - 状态为 active                                          │  │
│  │  - 时间范围重叠：existing.start < new.end AND              │  │
│  │                  existing.end > new.start                  │  │
│  └───────────────────────────────────────────────────────────┘  │
│                           │                                     │
│                           ▼                                     │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Step 2: 地点冲突检测 (可选)                               │  │
│  │  - 检查同一地点是否有其他会议                               │  │
│  │  - 检查地点之间的通勤时间                                   │  │
│  └───────────────────────────────────────────────────────────┘  │
│                           │                                     │
│                           ▼                                     │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Step 3: 冲突分级                                         │  │
│  │  ├── 完全冲突：时间完全重叠                                │  │
│  │  ├── 部分冲突：时间部分重叠                                │  │
│  │  └── 紧密相邻：间隔 < 15分钟                               │  │
│  └───────────────────────────────────────────────────────────┘  │
│                           │                                     │
│                           ▼                                     │
│  输出：冲突列表 + 冲突级别 + 建议方案                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 3.2.2 ConflictService 实现

```java
@Service
@Slf4j
public class ConflictService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CacheService cacheService;

    /**
     * 检测时间冲突
     */
    public ConflictResult checkConflict(Long userId, LocalDateTime startTime, 
                                         LocalDateTime endTime, Long excludeEventId) {
        String cacheKey = CacheKeyBuilder.conflictCheck(userId, startTime.toLocalDate().toString());

        return cacheService.getOrLoad(cacheKey, Duration.ofMinutes(5), () -> {
            // 查询可能冲突的事件
            List<CalendarEvent> potentialConflicts = eventRepository.findConflictingEvents(
                userId, startTime, endTime, excludeEventId
            );

            if (potentialConflicts.isEmpty()) {
                return ConflictResult.noConflict();
            }

            // 分析冲突级别
            List<ConflictDetail> conflicts = potentialConflicts.stream()
                .map(event -> analyzeConflict(event, startTime, endTime))
                .collect(Collectors.toList());

            return ConflictResult.of(conflicts);
        });
    }

    /**
     * 分析单个冲突
     */
    private ConflictDetail analyzeConflict(CalendarEvent existing, 
                                            LocalDateTime newStart, LocalDateTime newEnd) {
        LocalDateTime existStart = existing.getStartTime();
        LocalDateTime existEnd = existing.getEndTime();

        // 完全重叠
        if (!newStart.isAfter(existStart) && !newEnd.isBefore(existEnd)) {
            return new ConflictDetail(existing, ConflictLevel.FULL, "时间完全重叠");
        }

        // 部分重叠
        if (newStart.isBefore(existEnd) && newEnd.isAfter(existStart)) {
            Duration overlap = Duration.between(
                max(newStart, existStart), 
                min(newEnd, existEnd)
            );
            return new ConflictDetail(existing, ConflictLevel.PARTIAL, 
                "重叠 " + overlap.toMinutes() + " 分钟");
        }

        // 紧密相邻
        Duration gap = Duration.between(existEnd, newStart).abs();
        if (gap.toMinutes() < 15) {
            return new ConflictDetail(existing, ConflictLevel.ADJACENT, 
                "间隔仅 " + gap.toMinutes() + " 分钟");
        }

        return new ConflictDetail(existing, ConflictLevel.NONE, "无冲突");
    }
}
```

### 3.3 智能排期引擎

#### 3.3.1 排期算法设计

```
智能排期流程：
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  输入：待安排事件列表 + 时间范围 + 用户偏好                      │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Step 1: 收集约束条件                                     │  │
│  │  ├── 已有日程（不可移动）                                  │  │
│  │  ├── 用户偏好（工作时间、休息时间）                        │  │
│  │  ├── 事件优先级                                           │  │
│  │  └── 事件时长                                             │  │
│  └───────────────────────────────────────────────────────────┘  │
│                           │                                     │
│                           ▼                                     │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Step 2: 生成可用时间段                                   │  │
│  │  ├── 遍历日期范围                                         │  │
│  │  ├── 排除已有日程占用的时间                                │  │
│  │  ├── 排除休息时间                                         │  │
│  │  └── 生成空闲时间段列表                                    │  │
│  └───────────────────────────────────────────────────────────┘  │
│                           │                                     │
│                           ▼                                     │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Step 3: 事件匹配与排序                                   │  │
│  │  ├── 按优先级排序事件                                      │  │
│  │  ├── 高优先级事件优先安排在黄金时间                         │  │
│  │  ├── 考虑事件之间的缓冲时间                                │  │
│  │  └── 生成排期方案                                         │  │
│  └───────────────────────────────────────────────────────────┘  │
│                           │                                     │
│                           ▼                                     │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Step 4: 优化与调整                                       │  │
│  │  ├── 检查冲突                                             │  │
│  │  ├── 评估时间分布均匀性                                    │  │
│  │  ├── 生成多个备选方案                                      │  │
│  │  └── 计算每个方案的评分                                    │  │
│  └───────────────────────────────────────────────────────────┘  │
│                           │                                     │
│                           ▼                                     │
│  输出：推荐排期方案 + 备选方案 + 排期评分                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 3.3.2 ScheduleService 实现

```java
@Service
@Slf4j
public class ScheduleService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ConflictService conflictService;

    @Autowired
    private CacheService cacheService;

    /**
     * 智能排期建议
     */
    public ScheduleSuggestion suggestSchedule(Long userId, List<ScheduleRequest> requests, 
                                               LocalDate startDate, LocalDate endDate) {
        String cacheKey = CacheKeyBuilder.scheduleSuggestion(userId, 
            startDate.toString() + ":" + endDate.toString());

        return cacheService.getOrLoad(cacheKey, Duration.ofMinutes(10), () -> {
            // 1. 获取现有日程
            List<CalendarEvent> existingEvents = eventRepository.findByUserIdAndDateRange(
                userId, startDate.atStartOfDay(), endDate.atTime(23, 59)
            );

            // 2. 生成可用时间段
            List<TimeSlot> availableSlots = generateAvailableSlots(
                existingEvents, startDate, endDate
            );

            // 3. 按优先级排序请求
            List<ScheduleRequest> sortedRequests = requests.stream()
                .sorted(Comparator.comparingInt(ScheduleRequest::getPriority).reversed())
                .collect(Collectors.toList());

            // 4. 匹配时间段
            List<ScheduledEvent> scheduledEvents = new ArrayList<>();
            for (ScheduleRequest request : sortedRequests) {
                TimeSlot bestSlot = findBestSlot(availableSlots, request);
                if (bestSlot != null) {
                    scheduledEvents.add(new ScheduledEvent(request, bestSlot));
                    availableSlots.remove(bestSlot);
                }
            }

            // 5. 生成备选方案
            List<ScheduleAlternative> alternatives = generateAlternatives(
                sortedRequests, availableSlots, scheduledEvents
            );

            return new ScheduleSuggestion(scheduledEvents, alternatives, 
                calculateScore(scheduledEvents));
        });
    }

    /**
     * 生成可用时间段
     */
    private List<TimeSlot> generateAvailableSlots(List<CalendarEvent> existingEvents,
                                                    LocalDate startDate, LocalDate endDate) {
        List<TimeSlot> slots = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            // 工作时间：9:00-18:00
            LocalDateTime dayStart = date.atTime(9, 0);
            LocalDateTime dayEnd = date.atTime(18, 0);

            // 获取当天的已有事件
            List<CalendarEvent> dayEvents = existingEvents.stream()
                .filter(e -> e.getStartTime().toLocalDate().equals(date))
                .sorted(Comparator.comparing(CalendarEvent::getStartTime))
                .collect(Collectors.toList());

            // 生成空闲时间段
            LocalDateTime current = dayStart;
            for (CalendarEvent event : dayEvents) {
                if (current.isBefore(event.getStartTime())) {
                    slots.add(new TimeSlot(current, event.getStartTime()));
                }
                current = event.getEndTime();
            }
            if (current.isBefore(dayEnd)) {
                slots.add(new TimeSlot(current, dayEnd));
            }
        }

        return slots;
    }

    /**
     * 找到最佳时间段
     */
    private TimeSlot findBestSlot(List<TimeSlot> availableSlots, ScheduleRequest request) {
        return availableSlots.stream()
            .filter(slot -> slot.duration().toMinutes() >= request.getDurationMinutes())
            .min(Comparator.comparing(slot -> {
                // 评分：优先安排在上午，其次下午
                int hour = slot.start().getHour();
                int score = 0;
                if (hour >= 9 && hour <= 11) score = 10; // 上午黄金时间
                else if (hour >= 14 && hour <= 16) score = 8; // 下午黄金时间
                else if (hour >= 12 && hour <= 13) score = 3; // 午休时间
                else score = 5;
                return -score; // 分数越高越优先
            }))
            .orElse(null);
    }
}
```

### 3.4 百度云语音集成模块

#### 3.4.1 服务架构

```
百度云语音服务集成架构：
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    前端 (浏览器)                           │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │  │
│  │  │  录音模块   │  │  播放模块   │  │  UI 交互    │       │  │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘       │  │
│  │         │                │                │              │  │
│  │         └────────────────┼────────────────┘              │  │
│  │                          │                               │  │
│  │                    WebSocket 连接                        │  │
│  └──────────────────────────┼────────────────────────────────┘  │
│                             │                                   │
│                             ▼                                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                 Spring Boot 后端                           │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │  VoiceWebSocketHandler                              │  │  │
│  │  │  ├── 接收音频流 (PCM 格式)                          │  │  │
│  │  │  ├── 缓冲处理 (200ms/帧)                            │  │  │
│  │  │  └── 调用百度云 API                                  │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │                          │                                │  │
│  │                          ▼                                │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │  BaiduSpeechClient                                  │  │  │
│  │  │  ├── ASR 语音识别                                    │  │  │
│  │  │  │   ├── 短语音识别 (一句话)                         │  │  │
│  │  │  │   └── 实时语音识别 (流式)                         │  │  │
│  │  │  ├── TTS 语音合成                                    │  │  │
│  │  │  │   ├── 基础合成                                    │  │  │
│  │  │  │   └── 高品质合成                                  │  │  │
│  │  │  └── NLU 自然语言理解                                │  │  │
│  │  │      ├── 意图识别                                    │  │  │
│  │  │      └── 实体提取                                    │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
│                             │                                   │
│                             ▼                                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                  百度云 API 网关                           │  │
│  │  https://aip.baidubce.com                                 │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │  │
│  │  │  ASR API    │  │  TTS API    │  │  NLU API    │       │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘       │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 3.4.2 BaiduSpeechClient 实现

```java
@Component
@Slf4j
public class BaiduSpeechClient {

    @Value("${baidu.speech.app-id}")
    private String appId;

    @Value("${baidu.speech.api-key}")
    private String apiKey;

    @Value("${baidu.speech.secret-key}")
    private String secretKey;

    private String accessToken;
    private LocalDateTime tokenExpireTime;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 获取访问令牌
     */
    private synchronized String getAccessToken() {
        if (accessToken != null && LocalDateTime.now().isBefore(tokenExpireTime)) {
            return accessToken;
        }

        String url = String.format(
            "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=%s&client_secret=%s",
            apiKey, secretKey
        );

        Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);
        accessToken = (String) response.get("access_token");
        int expiresIn = (int) response.get("expires_in");
        tokenExpireTime = LocalDateTime.now().plusSeconds(expiresIn - 300);

        return accessToken;
    }

    /**
     * 短语音识别
     */
    public String recognizeSpeech(byte[] audioData, String format, int sampleRate) {
        String url = "https://vop.baidu.com/server_api";

        Map<String, Object> params = new HashMap<>();
        params.put("format", format);
        params.put("rate", sampleRate);
        params.put("channel", 1);
        params.put("cuid", "voice-calendar-" + System.currentTimeMillis());
        params.put("token", getAccessToken());
        params.put("speech", Base64.getEncoder().encodeToString(audioData));
        params.put("len", audioData.length);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);
        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

        if (response != null && (int) response.get("err_no") == 0) {
            List<String> results = (List<String>) response.get("result");
            return results.get(0);
        }

        log.error("Speech recognition failed: {}", response);
        return null;
    }

    /**
     * 流式语音识别 (WebSocket)
     */
    public void startStreamRecognition(WebSocketSession session, String sessionId) {
        // 建立与百度云的 WebSocket 连接
        String wsUrl = String.format(
            "wss://vop.baidu.com/realtime_asr?cuid=%s&token=%s",
            sessionId, getAccessToken()
        );

        // 创建 WebSocket 客户端连接
        WebSocketClient client = new StandardWebSocketClient();
        client.doHandshake(new StreamRecognitionHandler(session), wsUrl);
    }

    /**
     * 语音合成
     */
    public byte[] synthesizeSpeech(String text, String voiceType, int speed, int pitch) {
        String url = "https://tsn.baidu.com/text2audio";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("tex", text);
        params.add("tok", getAccessToken());
        params.add("cuid", "voice-calendar-" + System.currentTimeMillis());
        params.add("ctp", "1");
        params.add("lan", "zh");
        params.add("spd", String.valueOf(speed));
        params.add("pit", String.valueOf(pitch));
        params.add("vol", "5");
        params.add("per", voiceType);
        params.add("aue", "3"); // mp3 格式

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(
            url, HttpMethod.POST, request, byte[].class
        );

        return response.getBody();
    }
}
```

#### 3.4.3 百度云配置

```yaml
# application.yml
baidu:
  speech:
    app-id: ${BAIDU_APP_ID}
    api-key: ${BAIDU_API_KEY}
    secret-key: ${BAIDU_SECRET_KEY}

    # 语音识别配置
    asr:
      format: pcm
      sample-rate: 16000
      buffer-size: 200ms

    # 语音合成配置
    tts:
      voice-type: 0  # 0:女声 1:男声 3:男声 4:童声
      speed: 5       # 语速 0-15
      pitch: 5       # 音调 0-15
      volume: 5      # 音量 0-15
```

---

## 🐳 四、Docker 容器化部署

### 4.1 项目目录结构

```
voice-calendar/
├── docker/
│   ├── nginx/
│   │   ├── nginx.conf
│   │   └── ssl/
│   │       ├── cert.pem
│   │       └── key.pem
│   ├── mysql/
│   │   ├── init.sql
│   │   └── my.cnf
│   └── redis/
│       └── redis.conf
├── voice-calendar-frontend/
│   ├── Dockerfile
│   └── ...
├── voice-calendar-backend/
│   ├── Dockerfile
│   └── ...
├── docker-compose.yml
├── docker-compose.prod.yml
└── .env
```

### 4.2 Docker Compose 配置

```yaml
# docker-compose.yml
version: '3.8'

services:
  # Nginx 反向代理
  nginx:
    image: nginx:alpine
    container_name: voicecal-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./docker/nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./docker/nginx/ssl:/etc/nginx/ssl:ro
      - frontend-dist:/usr/share/nginx/html:ro
    depends_on:
      - frontend
      - backend
    networks:
      - voicecal-network
    restart: unless-stopped

  # Vue 3 前端
  frontend:
    build:
      context: ./voice-calendar-frontend
      dockerfile: Dockerfile
    container_name: voicecal-frontend
    volumes:
      - frontend-dist:/app/dist
    networks:
      - voicecal-network
    restart: unless-stopped

  # Spring Boot 后端
  backend:
    build:
      context: ./voice-calendar-backend
      dockerfile: Dockerfile
    container_name: voicecal-backend
    ports:
      - "8080:8080"
      - "8081:8081"  # WebSocket 端口
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - MYSQL_HOST=mysql
      - MYSQL_PORT=3306
      - MYSQL_DATABASE=voice_calendar
      - MYSQL_USERNAME=${MYSQL_USER}
      - MYSQL_PASSWORD=${MYSQL_PASSWORD}
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - BAIDU_APP_ID=${BAIDU_APP_ID}
      - BAIDU_API_KEY=${BAIDU_API_KEY}
      - BAIDU_SECRET_KEY=${BAIDU_SECRET_KEY}
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - voicecal-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  # MySQL 数据库
  mysql:
    image: mysql:8.0
    container_name: voicecal-mysql
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - MYSQL_DATABASE=voice_calendar
      - MYSQL_USER=${MYSQL_USER}
      - MYSQL_PASSWORD=${MYSQL_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
      - ./docker/mysql/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
      - ./docker/mysql/my.cnf:/etc/mysql/conf.d/my.cnf:ro
    networks:
      - voicecal-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis 缓存
  redis:
    image: redis:7-alpine
    container_name: voicecal-redis
    ports:
      - "6379:6379"
    command: redis-server /usr/local/etc/redis/redis.conf
    volumes:
      - redis-data:/data
      - ./docker/redis/redis.conf:/usr/local/etc/redis/redis.conf:ro
    networks:
      - voicecal-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  mysql-data:
  redis-data:
  frontend-dist:

networks:
  voicecal-network:
    driver: bridge
```

### 4.3 后端 Dockerfile

```dockerfile
# voice-calendar-backend/Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# 复制 Maven 配置
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# 下载依赖
RUN ./mvnw dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建应用
RUN ./mvnw package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 复制构建产物
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080 8081

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 4.4 前端 Dockerfile

```dockerfile
# voice-calendar-frontend/Dockerfile
FROM node:22-alpine AS builder

WORKDIR /app

# 复制 package.json
COPY package.json package-lock.json ./

# 安装依赖
RUN npm ci

# 复制源代码
COPY . .

# 构建应用
RUN npm run build

# Nginx 运行阶段
FROM nginx:alpine

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### 4.5 环境变量配置

```bash
# .env
# MySQL 配置
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_USER=voicecal
MYSQL_PASSWORD=your_password

# Redis 配置
REDIS_PASSWORD=your_redis_password

# 百度云配置
BAIDU_APP_ID=your_app_id
BAIDU_API_KEY=your_api_key
BAIDU_SECRET_KEY=your_secret_key

# 应用配置
APP_ENV=production
APP_SECRET=your_app_secret
```

### 4.6 Nginx 配置

```nginx
# docker/nginx/nginx.conf
events {
    worker_connections 1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;

    sendfile        on;
    keepalive_timeout  65;

    # 前端服务
    server {
        listen 80;
        server_name localhost;

        # 重定向到 HTTPS
        return 301 https://$server_name$request_uri;
    }

    server {
        listen 443 ssl;
        server_name localhost;

        ssl_certificate /etc/nginx/ssl/cert.pem;
        ssl_certificate_key /etc/nginx/ssl/key.pem;

        # 前端静态资源
        location / {
            root /usr/share/nginx/html;
            try_files $uri $uri/ /index.html;
        }

        # API 代理
        location /api/ {
            proxy_pass http://backend:8080/api/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # WebSocket 代理
        location /ws/ {
            proxy_pass http://backend:8081/ws/;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_set_header Host $host;
            proxy_read_timeout 86400;
        }
    }
}
```

### 4.7 部署命令

```bash
# 开发环境启动
docker-compose up -d

# 生产环境启动
docker-compose -f docker-compose.prod.yml up -d

# 查看日志
docker-compose logs -f backend

# 重建并启动
docker-compose up -d --build

# 停止并清理
docker-compose down -v

# 备份数据库
docker exec voicecal-mysql mysqldump -u root -p voice_calendar > backup.sql
```

---

## 📊 五、数据库设计 (增强版)

### 5.1 完整表结构

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS voice_calendar 
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE voice_calendar;

-- 用户表
CREATE TABLE `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password` VARCHAR(255) NOT NULL,
  `nickname` VARCHAR(50),
  `email` VARCHAR(100),
  `phone` VARCHAR(20),
  `avatar` VARCHAR(500),
  `preferences` JSON COMMENT '用户偏好设置',
  `status` TINYINT DEFAULT 1 COMMENT '1:正常 0:禁用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_username` (`username`),
  INDEX `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 日历事件表
CREATE TABLE `calendar_event` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `description` TEXT,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `location` VARCHAR(200),
  `category` VARCHAR(50) DEFAULT 'personal' COMMENT 'personal/work/family/other',
  `color` VARCHAR(20) DEFAULT '#409EFF',
  `is_all_day` BOOLEAN DEFAULT FALSE,
  `priority` TINYINT DEFAULT 5 COMMENT '优先级 1-10',
  `reminder_minutes` INT DEFAULT 15 COMMENT '提前提醒分钟数',
  `repeat_rule_id` BIGINT,
  `status` VARCHAR(20) DEFAULT 'active' COMMENT 'active/cancelled/completed',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_user_time` (`user_id`, `start_time`),
  INDEX `idx_user_status` (`user_id`, `status`),
  INDEX `idx_category` (`category`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 重复事件规则表
CREATE TABLE `repeat_rule` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `event_id` BIGINT NOT NULL,
  `rule_type` VARCHAR(20) NOT NULL COMMENT 'daily/weekly/monthly/yearly/custom',
  `interval_value` INT DEFAULT 1 COMMENT '间隔值',
  `days_of_week` VARCHAR(20) COMMENT '周几，逗号分隔',
  `day_of_month` INT COMMENT '每月几号',
  `month_of_year` INT COMMENT '每年几月',
  `end_date` DATETIME COMMENT '结束日期',
  `max_occurrences` INT COMMENT '最大重复次数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`event_id`) REFERENCES `calendar_event`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 提醒表
CREATE TABLE `reminder` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `event_id` BIGINT NOT NULL,
  `remind_at` DATETIME NOT NULL,
  `reminder_type` VARCHAR(20) DEFAULT 'notification' COMMENT 'notification/email/sms',
  `is_sent` BOOLEAN DEFAULT FALSE,
  `sent_at` DATETIME,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_remind_at` (`remind_at`, `is_sent`),
  FOREIGN KEY (`event_id`) REFERENCES `calendar_event`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 冲突记录表
CREATE TABLE `conflict_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `event_id_1` BIGINT NOT NULL,
  `event_id_2` BIGINT NOT NULL,
  `conflict_level` VARCHAR(20) NOT NULL COMMENT 'full/partial/adjacent',
  `overlap_minutes` INT,
  `resolution` VARCHAR(50) COMMENT 'ignored/moved/resolved',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user_events` (`user_id`, `event_id_1`, `event_id_2`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`event_id_1`) REFERENCES `calendar_event`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`event_id_2`) REFERENCES `calendar_event`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 智能排期记录表
CREATE TABLE `schedule_suggestion` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `suggestion_data` JSON COMMENT '排期建议详情',
  `score` DECIMAL(5,2) COMMENT '排期评分',
  `is_accepted` BOOLEAN,
  `accepted_at` DATETIME,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user_created` (`user_id`, `created_at`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 语音命令日志表
CREATE TABLE `voice_command_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `session_id` VARCHAR(100),
  `raw_audio_text` TEXT COMMENT '识别的原始文本',
  `intent` VARCHAR(50) COMMENT '识别的意图',
  `entities` JSON COMMENT '提取的实体',
  `command_result` VARCHAR(20) COMMENT 'success/failed/partial',
  `response_text` TEXT COMMENT '系统响应文本',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user_session` (`user_id`, `session_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 🔐 六、安全设计

### 6.1 安全架构

```
安全层次：
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  网络层安全                                               │  │
│  │  ├── HTTPS/WSS 加密传输                                   │  │
│  │  ├── CORS 跨域限制                                        │  │
│  │  └── 请求频率限制 (Rate Limiting)                         │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  应用层安全                                               │  │
│  │  ├── JWT 身份认证                                         │  │
│  │  ├── RBAC 权限控制                                        │  │
│  │  ├── 输入参数校验                                         │  │
│  │  └── SQL 注入防护                                         │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  数据层安全                                               │  │
│  │  ├── 敏感数据加密存储                                     │  │
│  │  ├── 数据库访问控制                                       │  │
│  │  └── 定期备份                                             │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 JWT 认证实现

```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userDetails.getId());
        claims.put("username", userDetails.getUsername());

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("Invalid JWT token");
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        return Long.parseLong(claims.get("userId").toString());
    }
}
```

---

## 📈 七、监控与运维

### 7.1 监控架构

```
监控体系：
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  应用监控 (Spring Boot Actuator)                          │  │
│  │  ├── /actuator/health      健康检查                       │  │
│  │  ├── /actuator/metrics     性能指标                       │  │
│  │  ├── /actuator/info        应用信息                       │  │
│  │  └── /actuator/env         环境变量                       │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  日志监控 (ELK Stack)                                     │  │
│  │  ├── Filebeat   日志收集                                  │  │
│  │  ├── Logstash   日志处理                                  │  │
│  │  ├── Elasticsearch 日志存储                               │  │
│  │  └── Kibana     日志可视化                                │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  性能监控                                                 │  │
│  │  ├── JVM 监控 (堆内存、GC、线程)                          │  │
│  │  ├── 数据库监控 (连接池、慢查询)                          │  │
│  │  ├── Redis 监控 (内存、命中率)                            │  │
│  │  └── WebSocket 监控 (连接数、消息量)                      │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 7.2 健康检查配置

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  endpoint:
    health:
      show-details: always
  health:
    redis:
      enabled: true
    db:
      enabled: true
    diskspace:
      enabled: true
```

---

## 🚀 八、部署方案

### 8.1 开发环境部署

```bash
# 1. 克隆项目
git clone https://github.com/your-repo/voice-calendar.git
cd voice-calendar

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env 文件，填入配置

# 3. 启动所有服务
docker-compose up -d

# 4. 查看服务状态
docker-compose ps

# 5. 访问应用
# 前端：http://localhost
# 后端 API：http://localhost/api
# WebSocket：ws://localhost/ws
```

### 8.2 生产环境部署

```bash
# 1. 配置生产环境变量
cp .env.production .env

# 2. 配置 SSL 证书
# 将证书放入 docker/nginx/ssl/

# 3. 启动生产环境
docker-compose -f docker-compose.prod.yml up -d

# 4. 配置域名解析
# 将域名指向服务器 IP

# 5. 配置防火墙
# 开放 80、443 端口
```

---

## 📋 九、技术栈总结

| 层次       | 技术               | 版本     | 用途     |
| -------- | ---------------- | ------ | ------ |
| **前端**   | Vue 3            | 3.4+   | 核心框架   |
|          | Vite             | 5.x    | 构建工具   |
|          | TypeScript       | 5.x    | 类型安全   |
|          | FullCalendar     | 6.x    | 日历组件   |
|          | Element Plus     | 2.x    | UI 组件库 |
| **后端**   | Spring Boot      | 3.2    | 核心框架   |
|          | Spring WebSocket | -      | 实时通信   |
|          | Spring Data JPA  | -      | 数据访问   |
|          | Spring Security  | -      | 安全框架   |
| **数据库**  | MySQL            | 8.0    | 主数据存储  |
|          | Redis            | 7.x    | 缓存加速   |
| **语音服务** | 百度云 ASR          | -      | 语音识别   |
|          | 百度云 TTS          | -      | 语音合成   |
| **部署**   | Docker           | 24.x   | 容器化    |
|          | Docker Compose   | 2.x    | 编排工具   |
|          | Nginx            | Alpine | 反向代理   |
| **监控**   | Actuator         | -      | 应用监控   |
|          | Prometheus       | -      | 指标收集   |
|          | Grafana          | -      | 可视化    |

---

## 🎯 十、总结

VoiceCal v1 版本在初始版基础上增加了以下核心能力：

1. **Redis 缓存加速** - 热点数据缓存，减少数据库压力
2. **Docker 容器化** - 一键部署，环境一致性保障
3. **智能冲突检测** - 时间冲突、地点冲突、紧密相邻检测
4. **智能排期引擎** - AI 驱动的最优时间安排建议
5. **百度云语音集成** - 流式识别、语音合成、自然语言理解

**技术可行性**：✅ 完全可行
**开发周期**：约 6-8 周（单人）
**运营成本**：约 ¥300-500/月

---

**文档版本**：v1.0
**创建日期**：2026-05-29
**适用环境**：Docker / JDK 17 / Node 22 / MySQL 8.0 / Redis 7
