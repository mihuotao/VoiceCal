<div 对齐="center">

# 🎙️ VoiceCal 智能语音日历

> **语音驱动 · 星空主题 · 全栈开源日历系统**

<!-- 视频链接 & 访问链接 — 大号字体优先展示 -->

## [🎬 演示视频 — Bilibili](https://www.bilibili.com/video/BV12qVU6EEni/?share_source=copy_web&vd_source=7b0b9cd479448e2aebb638edcb00abf6)

## [🌐 在线体验 — https://120.26.19.168](https://120.26.19.168)



**登录账号：admin/12345678

& lt;div>

---

## 概述

VoiceCal 是一个全栈智能语音日历系统，支持通过**中文语音命令**创建和管理日程，集成**百度实时流式语音识别 (ASR)** 与 **DeepSeek 大语言模型 NLU**，配合**星空玻璃拟态主题**提供沉浸式的日历管理体验。

前端采用 Vue 3   TypeScript   Vite 6，后端采用 Spring Boot 3   MyBatis-Plus   MySQL 8，全 Docker 一键部署。

---

## 功能特性

| 功能              | 描述                                       |
| --------------- | ---------------------------------------- |
| 🎤 **语音创建日程**   | 说出"下周三下午3点开项目评审会议"，自动解析并创建事件             |
| 📅 **智能日历网格**   | 月视图 7 列网格，支持农历 / 节日 / 节气 / 法定假日休班标注      |
| 🌟 **星空主题 UI**  | Canvas 动画星空（120 星 + 4 流星 + 3 星云）+ 玻璃拟态组件 |
| 🔐 **JWT 认证**   | 登录 / 注册 / Token 刷新，BCrypt 密码加密           |
| 🏮 **节日关怀**     | 32+ 中国节日 + 24 节气智能识别与祝福                  |
| ⏰ **事件提醒**      | 自定义提前提醒时间，浏览器通知推送                        |
| 🔁 **重复事件**     | 每日 / 每周 / 每月 / 每年循环规则                    |
| ⚡ **实时流式 ASR**  | 百度 WebSocket 实时语音识别，VAD 静音自动停止           |
| 🤖 **智能 NLU**   | 关键词 + DeepSeek LLM + 规则引擎三级意图解析          |
| 🖼️ **自定义背景**   | 用户上传图片作为日历背景（Base64 LocalStorage）        |
| 🌤️ **天气 / 时间** | 实时时钟显示 + 天气图标与温度                         |
| 🌙 **农历支持**     | 农历日期、节气、传统节日 using `lunar-javascript`    |
| 🎨 **主题定制**     | 玻璃模糊度 / 透明度 / 主色 / 渐变背景自定义               |

---

## 技术栈

### 前端

| 技术               | 版本     | 用途                                             |
| ---------------- | ------ | ---------------------------------------------- |
| Vue              | 3.5.13 | 响应式 UI 框架 (Composition API + `<script setup>`) |
| TypeScript       | ~5.8.3 | 类型安全严格模式                                       |
| Vite             | 6.3.4  | 构建工具                                           |
| Pinia            | 3.0.4  | 状态管理                                           |
| Axios            | 1.7.9  | HTTP 客户端                                       |
| motion-v         | 2.2.1  | 动画库                                            |
| lunar-javascript | 1.7.7  | 农历 / 节气计算                                      |
| @fullcalendar    | 6.1.15 | 日历组件（core, daygrid, timegrid, interaction）     |
| sass-embedded    | 1.87.0 | SCSS 预处理器                                      |

### 后端

| 技术           | 版本     | 用途                               |
| ------------ | ------ | -------------------------------- |
| Java         | 17     | 运行环境                             |
| Spring Boot  | 3.3.9  | 应用框架（web, websocket, validation） |
| MyBatis-Plus | 3.5.9  | ORM 框架                           |
| MySQL        | 8.0.42 | 数据库                              |
| JWT (jjwt)   | 0.12.6 | 认证令牌                             |
| Hutool       | 5.8.34 | 工具库（crypto, json）                |
| SpringDoc    | 2.5.0  | OpenAPI 接口文档                     |
| OkHttp       | 4.12.0 | 百度 WebSocket 客户端                 |
| Lombok       | -      | 代码精简                             |

### 外部服务

| 服务                   | 用途                    |
| -------------------- | --------------------- |
| 🎙️ **百度语音 (ASR)**   | 实时流式语音识别，WebSocket 协议 |
| 🔊 **百度语音 (TTS)**    | 文字转语音播报               |
| 🧠 **DeepSeek Chat** | 大语言模型 NLU 意图解析（降级方案）  |

---

## 项目结构

```
voicecal/
├── frontend/                        # Vue 3 前端
│   ├── src/
│   │   ├── components/
│   │   │   ├── auth/                # 登录注册
│   │   │   ├── calendar/            # 日历核心组件 (Grid/Cell/Header/InfoBar)
│   │   │   ├── voice/               # 语音管道 (Button/Overlay/Orb/Waveform)
│   │   │   ├── glass/               # 玻璃拟态组件 (Panel/Button/Dialog/Input)
│   │   │   ├── settings/            # 用户设置
│   │   │   ├── profile/             # 个人资料
│   │   │   └── search/              # 搜索
│   │   ├── composables/             # 组合式 API 逻辑 (13 个 composable)
│   │   ├── stores/                  # Pinia 状态 (theme)
│   │   ├── utils/                   # 工具函数 (request/holiday/lunar)
│   │   ├── types/                   # TypeScript 类型定义
│   │   └── styles/                  # SCSS 全局样式
│   ├── package.json
│   └── vite.config.ts
│
├── backend/                         # Spring Boot 后端
│   ├── src/main/java/com/voicecal/
│   │   ├── controller/              # REST 控制器 (12 个)
│   │   ├── voice/                   # 语音管道 (ASR/TTS/NLU)
│   │   ├── auth/                    # JWT 认证
│   │   ├── config/                  # Spring 配置
│   │   ├── service/                 # 业务逻辑层
│   │   ├── entity/                  # 数据实体 (9 个)
│   │   ├── mapper/                  # MyBatis-Plus Mapper
│   │   ├── model/dto/               # 数据传输对象
│   │   ├── model/vo/                # 视图对象
│   │   └── scheduled/               # 定时任务
│   ├── pom.xml
│   └── Dockerfile
│
├── db/                              # 数据库初始化脚本
│   ├── schema.sql                   # 9 张建表语句
│   └── init-data.sql                # 测试种子数据
│
├── deploy/                          # Docker 部署包
│   ├── docker-compose.yml           # 一键部署编排
│   ├── deploy.sh                    # 服务器部署脚本
│   ├── build.ps1                    # 本地打包脚本
│   ├── backend/                     # 后端镜像
│   ├── frontend/                    # 前端 nginx 镜像
│   └── db/                          # 初始化 SQL
│
├── docker-compose.yml               # 根编排文件（开发用）
└── docs/                            # 项目文档
```

---

## 快速开始（本地开发）

### 前置要求

- Node.js 22+
- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- npm 10+

### 1. 初始化数据库

```sql
-- 执行 db/schema.sql 创建库表
-- 执行 db/init-data.sql 插入测试数据
mysql -u root -p < db/schema.sql
mysql -u root -p < db/init-data.sql
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
# 启动于 http://localhost:8080
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
# 启动于 http://localhost:5173
```

登录账号：`admin` / `12345678`

---

## Docker 生产部署

> 服务器要求：Ubuntu 22.04 + Docker，2 核 4G 即可流畅运行。

### 一条命令部署

```bash
# 1. 将 deploy/ 目录上传到服务器
scp -r deploy root@120.26.19.168:/root/deploy

# 2. SSH 登录服务器，一键启动
ssh root@120.26.19.168 "cd /root/deploy && bash deploy.sh"
```

### 手动部署

```bash
cd /root/deploy
docker compose up -d --build
```

### 服务架构

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Frontend   │────▶│   Backend    │────▶│    MySQL     │
│  nginx:alpine │     │  JDK 17 JRE  │     │    8.0      │
│    Port 80   │     │   Port 8080  │     │   Port 3306 │
└──────────────┘     └──────────────┘     └──────────────┘
        │                    │
        │                    ▼
        │           ┌──────────────┐
        └──────────▶│  Baidu ASR   │
                    │  WebSocket   │
                    └──────────────┘
```

### 环境变量

| 变量                           | 说明               | 默认值                                         |
| ---------------------------- | ---------------- | ------------------------------------------- |
| `SPRING_DATASOURCE_URL`      | MySQL 连接地址       | `jdbc:mysql://mysql:3306/voice_calendar`    |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名           | `voicecal`                                  |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码            | `voicecal123`                               |
| `BAIDU_APP_ID`               | 百度语音 APP ID      | `123514570`                                 |
| `BAIDU_API_KEY`              | 百度语音 API Key     | -                                           |
| `BAIDU_SECRET_KEY`           | 百度语音 Secret Key  | -                                           |
| `DEEPSEEK_API_URL`           | DeepSeek API 地址  | `https://api.deepseek.com/chat/completions` |
| `DEEPSEEK_API_KEY`           | DeepSeek API Key | -                                           |

---

## API 概览

| 端点                      | 方法        | 说明             | 认证        |
| ----------------------- | --------- | -------------- | --------- |
| `/api/v1/auth/login`    | POST      | 用户登录           | 否         |
| `/api/v1/auth/register` | POST      | 用户注册           | 否         |
| `/api/v1/events`        | GET       | 查询事件列表         | 是         |
| `/api/v1/events`        | POST      | 创建事件           | 是         |
| `/api/v1/events/{id}`   | PUT/PATCH | 更新事件           | 是         |
| `/api/v1/events/{id}`   | DELETE    | 删除事件           | 是         |
| `/api/v1/voice/command` | POST      | 语音命令处理         | 是         |
| `/api/v1/voice/tts`     | POST      | 文字转语音          | 是         |
| `/api/v1/voice/asr`     | POST      | REST 语音识别      | 是         |
| `/api/v1/festivals`     | GET       | 查询节日           | 否         |
| `/api/v1/users/me`      | GET       | 当前用户信息         | 是         |
| `/api/v1/preferences`   | GET/PATCH | 用户偏好设置         | 是         |
| `/ws/voice`             | WS        | 实时语音 WebSocket | 是 (token) |

接口文档：`http://localhost:8080/swagger-ui.html` （本地开发）

---

## 语音管道架构

```
┌─────────────────────────────────────────────────────────┐
│                    前端 (Browser)                        │
│  getUserMedia → ScriptProcessorNode → Resample(48→16kHz)│
│         ↓                                                │
│  VAD (RMS>0.025) → Binary WebSocket (Int16 PCM)         │
│         ↓                                                │
│  partialText (实时转写) → finalResult (完整命令)         │
└───────────────┬─────────────────────────────────────────┘
                │ WebSocket /ws/voice?token=JWT
                ▼
┌─────────────────────────────────────────────────────────┐
│              后端 VoiceWebSocketHandler                  │
│  pendingBaiduSessions → audio buffer → flush on ready    │
│         ↓                                                │
│  BaiduStreamAsrService (OkHttp WebSocket)                │
│         ↓                                                │
│  FIN_TEXT → VoiceCommandService                          │
│         ↓                                                │
│  ┌─────────┐  ┌──────────┐  ┌─────────┐                │
│  │ Keyword  │  │ DeepSeek │  │  Rule   │                │
│  │ Match    │→ │ LLM NLU  │→ │ Engine  │                │
│  └─────────┘  └──────────┘  └─────────┘                │
│         ↓                                                │
│  Intent → CreateEventBuilder → Conflict Check → Save    │
└─────────────────────────────────────────────────────────┘
```

---

## 数据库设计

| 表名                      | 说明       | 关键字段                                 |
| ----------------------- | -------- | ------------------------------------ |
| `user`                  | 用户       | username, password (BCrypt), status  |
| `user_preference`       | 偏好设置     | default_view, tts_enabled, theme     |
| `calendar_event`        | 日历事件（核心） | title, start_time, end_time, user_id |
| `repeat_rule`           | 重复规则     | rule_type, interval, days_of_week    |
| `reminder`              | 提醒       | remind_at, minutes_before, is_sent   |
| `festival`              | 节日       | name, date, type, greeting           |
| `voice_command_log`     | 语音日志     | intent, raw_audio_text, nlu_source   |
| `festival_greeting_log` | 节日关怀日志   | greeting_text, user_action           |
| `audit_log`             | 操作审计     | action, entity_type, details (JSON)  |

---

## 贡献指南

欢迎贡献代码！请按以下流程操作：

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: add some feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

### 开发规范

- 前端：Vue 3 Composition API + `<script setup>` + TypeScript 严格模式
- 后端：Spring Boot 3 + MyBatis-Plus + 统一响应 `ApiResult<T>`
- 提交信息：遵循 [Conventional Commits](https://www.conventionalcommits.org/)

---

## 路线图

- [x] 日历网格 + 农历 + 节日 + 休班标注
- [x] JWT 认证 + 用户管理
- [x] 事件 CRUD + 冲突检测 + 重复事件
- [x] 百度实时流式 ASR + WebSocket 桥接
- [x] 三级 NLU 意图解析（关键词 + LLM + 规则）
- [x] 星空主题玻璃拟态 UI
- [x] Docker 一键部署
- [ ] 

---

## 许可证

[MIT License](LICENSE) © 2026 VoiceCal

---

<div align="center">

**如果 VoiceCal 对你有帮助，欢迎 ⭐ Star 支持！**

[⬆ 回到顶部](#-voicecal-智能语音日历)

</div>
