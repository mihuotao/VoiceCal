# VoiceCal — 智能语音日历 v5（交互·动态·联网）

> **版本**: VoiceCal_v5  
> **创建日期**: 2026-05-29  
> **开发模式**: AI Agent 辅助 + 单人审核  
> **开发周期**: 2 天（高强度）

---

## 一、项目概述

### 1.1 项目背景

在快节奏的现代生活中，日程管理已成为每个人的日常需求。传统日历工具依赖手动输入，在驾驶、烹饪、运动等场景下操作不便。VoiceCal 通过**语音优先（Voice-First）** 的交互范式，让用户"动口不动手"即可完成日程管理。

### 1.2 核心价值

| 价值点 | 说明 |
|--------|------|
| **效率提升** | 语音操作比手动输入快 3-5 倍 |
| **场景解放** | 支持 hands-free 操作 |
| **智能辅助** | AI 驱动的冲突检测与排期优化 |
| **多模态反馈** | 语音 + 视觉 + **弹簧物理动效** 三重反馈 |
| **沉浸交互** | 毛玻璃视效 + **手势动画 + 粒子效果** |
| **联网扩展** | **语音搜索互联网信息，智能关联日程** |

### 1.3 本机环境

| 项目 | 规格 | 评估 |
|------|------|------|
| 操作系统 | Windows 11 | ✅ |
| CPU | i5-13500H (12核/16线程) | ✅ 并行编译高效 |
| 内存 | 16GB | ✅ |
| JDK | 17.0.12 LTS | ✅ Spring Boot 3.x |
| Node.js | 22.16.0 | ✅ Vite 6 |
| MySQL | 8.0.42 | ✅ |
| Maven | 3.8.8 | ✅ |
| AI Agent | opencode + 联网搜索 | ✅ 辅助生成代码 |

---

## 二、设计理念

### 2.1 视觉风格 — zhima 日历全透明毛玻璃

- **全透明背景**：桌面融合，无传统白色卡片感
- **`backdrop-filter: blur()`**：可调节模糊度 (8px–48px)
- **可调透明度**：`rgba(255,255,255,0.05–0.25)`
- **深色基底**：渐变深色背景衬托玻璃质感
- **自定义主题色**：用户自由搭配玻璃色调
- **农历/节气显示**：lunar-javascript 渲染

### 2.2 语音交互 — Siri Orb 视觉中心

- **Canvas 2D 语音球**：三态动画（空闲呼吸→录音流光→处理旋转）
- **全屏语音覆盖层**：slide-up 毛玻璃面板 + 实时波形
- **音频响应形变**：simplex-noise 驱动球体形变
- **三种尺寸自适应**：80px（工具栏）/ 120px（默认）/ 160px（全屏）

### 2.3 交互与动态 — 2026 前沿动效

| 效果 | 实现方式 |
|------|----------|
| **弹簧物理按钮** | `motion-v` `whileHover` + `whileTap` spring |
| **日历网格 hover 升起** | CSS transform + spring overshoot |
| **点击 cell zoom** | `motion-v` `layout` + scale animation |
| **月份切换形变过渡** | CSS View Transitions API + slide |
| **拖拽事件弹簧跟随** | `motion-v` drag + spring physics |
| **语音球呼吸脉冲** | Canvas RAF scale 1↔1.05 |
| **录音粒子环绕** | Canvas 粒子系统飘散 |
| **处理态旋转光环** | Canvas rotate ring |
| **面板入场玻璃渐显** | CSS `glass-in` keyframes |
| **悬浮微浮动** | `motion-v` spring float |
| **全屏覆盖 slide-up** | `motion-v` AnimatePresence |
| **波形实时渲染** | Canvas 多正弦波（Siri 风格） |
| **工具栏磁吸弹性** | `motion-v` spring + layout |
| **语音按钮形变** | idle↔recording 圆↔方 `borderRadius` spring |
| **视差滚动** | CSS `animation-timeline: scroll()` |
| **事件增删过渡** | `motion-v` AnimatePresence list |
| **手势翻月** | swipe left/right 切换月份 + spring |

### 2.4 联网搜索能力

- **语音触发搜索**："搜索明天的天气"、"查一下这个餐厅"
- **搜索结果关联日程**：天气信息自动添加备注，餐厅信息创建事件
- **Web Search API 集成**：调用搜索服务获取实时信息
- **结果预览弹窗**：玻璃浮层展示搜索结果，支持一键转日程

---

## 三、系统架构

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户终端 (浏览器)                           │
│  录音 (MediaRecorder) | VoiceOrb Canvas | FullCalendar 玻璃日历   │
│  弹簧动效 (motion-v) | View Transitions | 粒子系统               │
│                              │                                   │
│          Composables: useVoice / useWebSocket / useEvents        │
│                      useTheme / useSearch                        │
└──────────────────────────────┼───────────────────────────────────┘
                         HTTPS / WSS
                                │
┌──────────────────────────────┼───────────────────────────────────┐
│                       Nginx (反向代理 + SSL)                      │
└──────────────────────────────┼───────────────────────────────────┘
                                │
┌──────────────────────────────┼───────────────────────────────────┐
│                     Spring Boot 3.3.9 (单体应用)                  │
│  Controller → Service → Repository → MySQL / Redis / 百度云API   │
│  WebSocket Handler → 音频流处理 → 实时推送                        │
└──────────────────────────────┬───────────────────────────────────┘
                                │
                     ┌──────────┴──────────┐
                     ▼                     ▼
               ┌──────────┐         ┌──────────┐
               │  MySQL   │         │  百度云  │
               │  持久化   │         │ ASR/TTS │
               └──────────┘         └──────────┘
```

### 3.2 核心链路（语音创建事件 + 联网搜索）

```
录音 → WebSocket 音频流 → 百度云 ASR → NLU 意图解析
                                          ↓
                                    ┌──────────┐
                                    │ 联网搜索  │ ← 语音指令含搜索意图
                                    └──────────┘
                                          ↓
                                   搜索结果预览 → 一键转日程
                                          ↓
                                   冲突检测 → 节日关怀检测
                                          ↓
                                   预览确认 → 写入数据库
                                          ↓
                                   TTS 播报 + 日历刷新 + 动效反馈
```

---

## 四、技术选型

### 4.1 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Vue 3** | 3.5+ | 核心框架（Composition API） |
| **Vite** | 6.x | 构建工具 |
| **TypeScript** | 5.8+ | 类型安全（strict 模式） |
| **Pinia** | 3.x | 状态管理 |
| **FullCalendar** | 6.1+ | 日历可视化（仅逻辑层，渲染全覆盖） |
| **motion-v** | 12.x | **弹簧物理动画引擎**（手势/布局/滚动/退出） |
| **CSS View Transitions** | 原生 | **月份切换形变过渡** |
| **Canvas 2D** | 原生 | VoiceOrb + 粒子 + 波形渲染 |
| **lunar-javascript** | 1.7.7 | 农历/节气/节日计算 |
| **Axios** | 1.7+ | HTTP 客户端 |
| **sass-embedded** | 1.87+ | SCSS 预处理器 |

### 4.2 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.9 | 核心框架 |
| Java | 17+ | 开发语言 |
| MyBatis-Plus | 3.5.9 | ORM |
| MySQL Connector | 8.x | 数据库驱动 |
| Spring WebSocket | - | 实时通信（二进制音频流） |
| Spring Security | - | JWT 认证 |
| JJWT | 0.12.6 | JWT 令牌 |
| Lombok | - | 消除样板代码 |

### 4.3 云服务

| 服务 | 提供商 | 用途 | 免费额度 |
|------|--------|------|----------|
| ASR 短语音识别 | 百度云 | 语音转文字 | 每日 5 万次 |
| TTS 语音合成 | 百度云 | 文字转语音 | 免费额度充足 |
| **Web Search** | **SerpAPI / 百度搜索API** | **联网搜索** | 每月 100 次免费 |

### 4.4 UI 组件策略

**无 UI 组件库**，全自研毛玻璃组件库：

| 组件 | 用途 |
|------|------|
| `GlassPanel.vue` | 毛玻璃容器（可调透明度/模糊/颜色） |
| `GlassButton.vue` | 弹簧物理按钮（hover/tap 动画） |
| `GlassDialog.vue` | 毛玻璃弹窗（入场/出场动画） |
| `GlassInput.vue` | 玻璃风格输入框 |
| `GlassTooltip.vue` | 玻璃提示 |

---

## 五、创新点

### 5.1 创新点一：弹簧物理驱动 UI

**创新描述**：摒弃传统 CSS ease 过渡，全界面采用 `motion-v` 弹簧物理引擎。每个交互元素都有「重量感」——按钮按压回弹、卡片悬浮过冲、拖拽惯性跟随。

**技术实现**：
- `stiffness: 400, damping: 15` 弹簧参数统一配置
- `useSpring` 组合式函数封装
- 手势识别（hover/tap/drag/pan）跨设备兼容

**创新价值**：
- 交互从「机械」变「有机」
- 用户感知到界面的「生命力」
- 2026 年高端应用的标志性体验

### 5.2 创新点二：CSS View Transitions 日历导航

**创新描述**：月份切换时，不依赖 JS 动画库，使用原生 CSS View Transitions API 实现旧月→新月的平滑形变过渡。

**技术实现**：
```css
@view-transition { navigation: auto; }
::view-transition-old(root) { animation: slide-out-to-left 0.3s; }
::view-transition-new(root) { animation: slide-in-from-right 0.3s; }
```

**创新价值**：
- 原生 GPU 加速，60fps 流畅
- 零 JS 开销
- 浏览器原生支持，2026 年覆盖 90%+ 用户

### 5.3 创新点三：语音球三态粒子系统

**创新描述**：Siri Orb 语音球不仅是一个发光球体，还包含粒子系统——空闲时微尘漂浮、录音时光子环绕、处理时环形旋转。

**技术实现**：
- Canvas 2D `requestAnimationFrame` 循环
- simplex-noise 有机形变
- 粒子池 + 向量场计算
- 状态机驱动三态切换

**创新价值**：
- 视觉上「语音正在工作」一目了然
- 沉浸感远超传统图标
- 差异化竞争优势

### 5.4 创新点四：联网语音搜索

**创新描述**：用户语音可以触发联网搜索，将实时信息无缝融入日程管理。

**典型场景**：
- "这周末北京天气怎么样" → 搜索天气 → 创建带天气备注的事件
- "搜索附近川菜馆" → 搜索结果弹窗 → 选择后创建晚餐事件
- "帮我查一下端午节的由来" → TTS 播报搜索结果

**技术实现**：
- 前端 `useSearch` composable 封装搜索 API
- 后端 `SearchController` 代理外部搜索服务
- 搜索结果解析 → 事件模板匹配
- 玻璃浮层展示 + 一键转日程按钮

**创新价值**：
- 日历从「记录工具」升级为「生活助手」
- 减少用户 App 间切换
- 语音交互的天然延伸

### 5.5 创新点五：流式语音实时识别

同 v1-v3 方案。

### 5.6 创新点六：中国式浪漫·情感化节日关怀

同 v1-v3 方案。

---

## 六、数据库设计

### 6.1 核心表结构

```sql
-- 用户表
CREATE TABLE `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password` VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密',
  `nickname` VARCHAR(50),
  `email` VARCHAR(100),
  `preferences` JSON COMMENT '偏好设置（含玻璃参数/主题色）',
  `status` TINYINT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 日历事件表
CREATE TABLE `calendar_event` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `description` TEXT,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `all_day` TINYINT(1) DEFAULT 0,
  `location` VARCHAR(200),
  `category` VARCHAR(50) DEFAULT 'personal',
  `color` VARCHAR(20) DEFAULT '#6366f1',
  `priority` TINYINT DEFAULT 5,
  `reminder_minutes` INT DEFAULT 15,
  `status` VARCHAR(20) DEFAULT 'active',
  `search_cache` JSON COMMENT '联网搜索缓存结果',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 语音命令日志表
CREATE TABLE `voice_command_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `session_id` VARCHAR(100),
  `raw_audio_text` TEXT,
  `intent` VARCHAR(50),
  `entities` JSON,
  `confidence` DECIMAL(3,2),
  `command_result` VARCHAR(20),
  `response_text` TEXT,
  `duration_ms` INT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 6.2 索引设计

| 表 | 索引 | 用途 |
|----|------|------|
| calendar_event | idx_user_time (user_id, start_time) | 用户日程查询 |
| calendar_event | idx_user_status (user_id, status) | 状态筛选 |
| voice_command_log | idx_user_session (user_id, session_id) | 会话查询 |

---

## 七、接口设计

### 7.1 RESTful API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/login` | 用户登录 |
| GET | `/api/events?start=&end=` | 按时间范围查询 |
| GET | `/api/events/{id}` | 查询事件详情 |
| POST | `/api/events` | 创建事件 |
| PUT | `/api/events/{id}` | 更新事件 |
| DELETE | `/api/events/{id}` | 删除事件 |
| POST | `/api/voice/asr` | 语音识别 |
| POST | `/api/voice/nlu` | NLU 意图解析 |
| POST | `/api/voice/command` | 语音指令全链路 |
| GET | `/api/schedule/conflicts` | 冲突检测 |
| **POST** | **`/api/search/web`** | **联网搜索（代理）** |

### 7.2 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1716998400000
}
```

### 7.3 WebSocket 接口

| 端点 | 方向 | 消息类型 | 说明 |
|------|------|----------|------|
| `/ws/voice` | 客户端→服务端 | Binary | 音频流（PCM 16kHz） |
| `/ws/voice` | 服务端→客户端 | Text | ASR 识别结果 |
| `/ws/calendar` | 服务端→客户端 | Text | 事件变更推送 + 提醒 |

### 7.4 联网搜索接口

```
POST /api/search/web
请求: { "query": "明天北京天气", "type": "weather|place|general" }
响应: {
  "results": [
    { "title": "北京天气预报", "snippet": "明日晴转多云...", "url": "..." }
  ],
  "summary": "明天北京天气晴转多云，气温15-25°C"
}
```

---

## 八、2 天开发计划

### 总体策略

- **AI Agent 辅助生成代码**，人工审核 + 修正
- **数据库先行**，初始化数据即生成
- **motion-v 弹簧动效贯穿全程**，每个步骤都包含动效交付
- **语音球和联网搜索为攻坚重点**
- **Code Review + 联调** 收尾

### 第 1 天：基础设施 + 玻璃组件 + 动效引擎 + 日历 + 语音球

| 时间段 | 任务 | 产出物 | 动效要点 |
|--------|------|--------|----------|
| **09:00-09:30** | 项目初始化 + 设计令牌 | Vite 6 + Vue 3.5 + TS + SCSS | 安装 `motion-v` |
| **09:30-10:00** | 毛玻璃组件库 | GlassPanel/Button/Dialog/Input | 弹簧 hover/tap 动画 |
| **10:00-10:30** | CSS View Transitions | 月份切换形变过渡 | slide 动画配置 |
| **10:30-11:00** | useTheme + 主题 store | 透明度/模糊/颜色实时调节 | 颜色过渡动画 |
| **11:00-12:00** | **VoiceOrb Canvas 引擎** | Siri 风格语音球 | 三态粒子系统 |
| **12:00-13:00** | 休息 | - | - |
| **13:00-14:00** | VoiceButton + 全屏覆盖 | 语音入口 + 覆盖层 UI | 圆↔方形变 + slide-up |
| **14:00-15:00** | 透明日历网格 + FullCalendar | 日历渲染（日/周/月） | hover 升起 + click zoom |
| **15:00-16:00** | 前端事件 CRUD 联调 | Axios + 事件列表/创建/编辑 | AnimatePresence 列表过渡 |
| **16:00-17:00** | 后端事件 CRUD | EventController + Service | - |
| **17:00-18:00** | 拖拽事件 + 手势翻月 | drag-drop + swipe | 弹簧物理跟随 |

### 第 2 天：语音管道 + 联网搜索 + 节日 + 设置 + 认证 + 联调

| 时间段 | 任务 | 产出物 | 动效要点 |
|--------|------|--------|----------|
| **09:00-10:30** | **语音核心攻坚** | 前端录音 + WebSocket + ASR | 语音球音频响应形变 |
| **10:30-11:00** | NLU 引擎（规则引擎） | IntentRecognizer + TimeParser | - |
| **11:00-11:30** | 冲突检测 | ConflictService | 冲突面板弹入动画 |
| **11:30-12:00** | **联网搜索** | SearchController + useSearch | 搜索结果玻璃浮层 |
| **12:00-13:00** | 休息 | - | - |
| **13:00-14:00** | 节日关怀 | FestivalService + FestivalCard | 祝福卡片 spring pop |
| **14:00-14:30** | TTS 语音播报 | Web Speech API + 百度云 TTS | - |
| **14:30-15:00** | 底部工具栏 + 笔记 | 6 图标工具栏 + 双击内联笔记 | 磁吸弹性 + 笔记淡入 |
| **15:00-15:30** | 设置面板 | 玻璃参数/主题色/壁纸 | 滑块弹簧实时预览 |
| **15:30-16:00** | 用户认证 | 登录/注册页面 | 页面过渡动画 |
| **16:00-17:00** | **Code Review** | 前后端代码审查 | - |
| **17:00-18:00** | **前后端联调** | 语音→NLU→创建→搜索全链路 | 修复动效问题 |

### 核心攻坚时序图

```
Day 1                                Day 2
┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
│基础  │玻璃  │View  │主题  │语音  │日历  │CRUD │语音  │联网  │节日  │
│设施  │组件  │Trans │Store │球    │网格  │联调  │管道  │搜索  │关怀  │
│     │     │      │      │     │     │     │     │     │     │
│     │     │      │      ├─────┤     │     ├─────┤     │     │
│     │     │      │      │手势  │     │     │波形  │     │     │
│     │     │      │      │翻月  │     │     │渲染  │     │     │
│     │     │      │      │拖拽  │     │     │     │     │     │
└─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
          ║ 动效贯穿全程 ║
     motion-v spring / View Transitions / 粒子 / Canvas
```

---

## 九、项目目录结构

```
calendar/
├── docs/
│   ├── 开发方案.md
│   ├── api-document.md
│   └── VoiceCal_v5.md              # 本文件
│
├── db/
│   ├── schema.sql
│   └── init-data.sql
│
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/voicecal/
│       ├── VoiceCalApplication.java
│       ├── config/                  # WebSocket / Security / Cors
│       ├── controller/              # Event / Auth / Voice / Search
│       ├── websocket/               # VoiceHandler / CalendarHandler
│       ├── service/                 # Event / Voice / Nlu / Festival / Search
│       ├── repository/              # Event / User / VoiceLog
│       ├── entity/                  # CalendarEvent / User / VoiceCommandLog
│       ├── dto/                     # Request / Response / ApiResponse
│       ├── client/                  # BaiduSpeechClient / SearchClient
│       ├── nlu/                     # IntentRecognizer / TimeParser
│       ├── security/                # JwtProvider / JwtFilter
│       └── exception/               # GlobalExceptionHandler
│
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── index.html
│   └── src/
│       ├── main.ts
│       ├── App.vue
│       ├── styles/
│       │   ├── _variables.scss      # 设计令牌
│       │   ├── _glass.scss          # 玻璃宏
│       │   ├── _animations.scss     # 动效
│       │   ├── _reset.scss          # 重置
│       │   └── main.scss            # 入口
│       ├── components/
│       │   ├── glass/               # GlassPanel / GlassButton / GlassDialog / GlassInput
│       │   ├── voice/               # VoiceOrb / VoiceButton / VoiceWaveform / VoiceResult / VoiceOverlay
│       │   ├── calendar/            # CalendarView / CalendarCell / EventDetail / EventForm
│       │   ├── festival/            # FestivalCard / FestivalSuggestion
│       │   ├── search/              # SearchPanel / SearchResultCard
│       │   └── common/              # LoadingSpinner / Toast / ParticleCanvas
│       ├── composables/
│       │   ├── useTheme.ts          # 主题 + 玻璃参数
│       │   ├── useVoice.ts          # 录音 + 语音状态
│       │   ├── useWebSocket.ts      # WebSocket 连接
│       │   ├── useEvents.ts         # 事件 CRUD
│       │   ├── useSearch.ts         # 联网搜索
│       │   ├── useMotion.ts         # 弹簧动画统一配置
│       │   └── useCalendar.ts       # 日历逻辑
│       ├── stores/
│       │   ├── calendar.ts          # Pinia 日历 store
│       │   ├── voice.ts             # 语音状态 store
│       │   ├── theme.ts             # 主题 store
│       │   └── user.ts              # 用户 store
│       ├── types/
│       │   ├── event.ts
│       │   ├── voice.ts
│       │   ├── ws.ts
│       │   ├── theme.ts
│       │   └── search.ts
│       └── utils/
│           ├── audio.ts
│           ├── date.ts
│           ├── constants.ts
│           └── lunar.ts
│
└── docker-compose.yml
```

---

## 十、开发规范

### 10.1 命名规范

| 层级 | 规范 | 示例 |
|------|------|------|
| 后端 Controller | XxxController | EventController |
| 后端 Service | XxxService | SearchService |
| 后端 Repository | XxxRepository | EventRepository |
| 后端 DTO | XxxRequest / XxxResponse | EventCreateRequest |
| 前端组件 | PascalCase | GlassPanel.vue |
| 前端 Composable | useXxx | useSearch.ts |
| API 路径 | 小写 + 复数 | /api/search/web |
| 数据库表 | 小写 + 下划线 | voice_command_log |

### 10.2 动效规范

| 类型 | 参数 | 用途 |
|------|------|------|
| 弹簧 stiffness | 400 | 按钮 hover |
| 弹簧 stiffness | 600 | 点击 tap |
| 弹簧 damping | 15 | 通用（轻微过冲） |
| 弹簧 damping | 25 | 保守（几乎无过冲） |
| View Transitions | 300ms | 月份切换 |
| CSS fade-in | 400ms | 面板入场 |
| 粒子生命 | 2s | 语音球环绕 |
| Canvas RAF | 60fps | 语音球渲染 |

### 10.3 AI Agent 协作规范

| 阶段 | 交给 Agent 生成 | 人工负责 |
|------|----------------|----------|
| 数据库 | DDL + 初始化 SQL | 审核表结构 |
| 后端 | Entity/Service/Controller | 审核业务逻辑 |
| 前端 | 组件 + Composable + Store | 审核交互逻辑 |
| 动效 | 弹簧参数 + 动画配置 | 审核动效体验 |
| 语音 | WebSocket + 录音 + ASR | 审核音频流 |
| 联网搜索 | SearchClient + useSearch | 审核搜索逻辑 |
| CR | 代码质量扫描 | 确认修正 |

---

## 十一、风险与应对

| 风险 | 等级 | 应对策略 |
|------|------|----------|
| 2 天时间极其紧张 | 高 | Agent 生成+人工审核并行；先跑通主链路 |
| motion-v 集成兼容性 | 中 | 提前验证 Vue 3.5 + motion-v 版本兼容 |
| View Transitions 浏览器支持 | 中 | 降级为纯 CSS 过渡动画 |
| 百度云 ASR 配置耗时 | 中 | 提前注册；先 Mock ASR |
| 联网搜索 API 限额 | 中 | 缓存搜索结果；降级为本地搜索 |
| Canvas 性能（60fps） | 低 | 粒子数控制（<200）；离屏 Canvas 优化 |
| WebSocket 联调 | 低 | 先使用在线测试工具验证后端 |

---

## 十二、交付标准

| 维度 | 标准 |
|------|------|
| 语音创建事件 | 录音 → ASR → NLU → 创建 → 日历刷新（含动效） |
| 语音查询事件 | "今天有什么安排" → 日历定位 + TTS 播报 |
| 联网搜索 | "搜索天气" → 搜索玻璃面板 → 一键转日程 |
| 节日关怀 | 节日首次创建日程 → 弹窗祝福（spring pop） |
| 冲突检测 | 创建事件时自动检测 → 动画提示 |
| 弹簧动效 | 按钮/卡片/弹窗全界面 spring 物理反馈 |
| 月份切换 | CSS View Transitions 平滑形变 |
| 语音球三态 | 呼吸/流光/旋转状态正确切换 |
| 日历可视化 | 日/周/月视图正常展示，透明玻璃风格 |

---

**文档版本**: v5.0  
**基准版本**: VoiceCal_v5  
**创建日期**: 2026-05-29  
**开发模式**: AI Agent 辅助  
**开发周期**: 2 天（高强度）
