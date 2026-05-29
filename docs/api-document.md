# VoiceCal 智能语音日历 — API 接口文档

**版本**: v1.1  
**基础URL**: `http://localhost:8080/api/v1`  
**字符编码**: UTF-8  
**数据格式**: JSON

---

## 目录

1. [通用规范](#1-通用规范)
2. [认证模块](#2-认证模块)
3. [用户模块](#3-用户模块)
4. [偏好设置模块](#4-偏好设置模块)
5. [日历事件模块](#5-日历事件模块)
6. [重复规则模块](#6-重复规则模块)
7. [提醒模块](#7-提醒模块)
8. [节日模块](#8-节日模块)
9. [语音命令模块](#9-语音命令模块)
10. [语音服务模块（Baidu Cloud）](#10-语音服务模块baidu-cloud)
11. [审计日志模块](#11-审计日志模块)
12. [附录](#12-附录)

---

## 1. 通用规范

### 1.1 认证方式

Bearer JWT，在请求头中携带：

```
Authorization: Bearer <token>
```

### 1.2 通用请求头

| 字段             | 值                    | 必填 | 说明         |
| ---------------- | --------------------- | ---- | ------------ |
| Content-Type     | application/json      | 是   | 请求体格式   |
| Authorization    | Bearer \<token\>      | 是*  | 登录后必填   |

### 1.3 通用响应结构

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1717056000000
}
```

**分页响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [ ... ],
    "page": 1,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  },
  "timestamp": 1717056000000
}
```

**错误响应**:
```json
{
  "code": 400,
  "message": "请求参数错误",
  "errors": [
    { "field": "title", "message": "标题不能为空" }
  ],
  "timestamp": 1717056000000
}
```

### 1.4 HTTP 状态码说明

| 状态码 | 含义               | 说明                             |
| ------ | ------------------ | -------------------------------- |
| 200    | OK                 | 请求成功                         |
| 201    | Created            | 创建成功                         |
| 204    | No Content         | 删除成功（无响应体）             |
| 400    | Bad Request        | 参数错误或校验失败               |
| 401    | Unauthorized       | 未认证或 Token 过期              |
| 403    | Forbidden          | 无权限                           |
| 404    | Not Found          | 资源不存在                       |
| 409    | Conflict           | 资源冲突（如用户名重复）         |
| 422    | Unprocessable Entity | 业务逻辑错误                   |
| 429    | Too Many Requests  | 请求频率超限                     |
| 500    | Internal Server Error | 服务器内部错误                 |

### 1.5 分页查询参数

| 参数      | 类型   | 默认值 | 说明                 |
| --------- | ------ | ------ | -------------------- |
| page      | int    | 1      | 页码（从 1 开始）    |
| size      | int    | 20     | 每页条数（最大 100） |
| sort      | string | -      | 排序字段，如 `startTime,desc` |

---

## 2. 认证模块

### 2.1 用户注册

`POST /auth/register`

**Request Body**:
```json
{
  "username": "zhangsan",
  "password": "Abc123456",
  "nickname": "张三",
  "email": "zhangsan@example.com",
  "phone": "13800138000"
}
```

| 字段     | 类型   | 必填 | 约束                       |
| -------- | ------ | ---- | -------------------------- |
| username | string | 是   | 4-20 位字母数字下划线      |
| password | string | 是   | 8-32 位，含字母和数字      |
| nickname | string | 否   | 2-20 位                    |
| email    | string | 否   | 合法邮箱格式               |
| phone    | string | 否   | 11 位手机号                |

**Response 201**:
```json
{
  "code": 201,
  "message": "注册成功",
  "data": {
    "userId": 1,
    "username": "zhangsan"
  }
}
```

**错误码**:
| 状态码 | message        | 场景               |
| ------ | -------------- | ------------------ |
| 409    | 用户名已存在   | username 重复      |
| 409    | 邮箱已被使用   | email 重复         |
| 400    | 参数校验失败   | 格式不合法         |

### 2.2 用户登录

`POST /auth/login`

**Request Body**:
```json
{
  "username": "zhangsan",
  "password": "Abc123456"
}
```

**Response 200**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "userId": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "lastLoginAt": "2026-05-29T10:30:00"
  }
}
```

**错误码**:
| 状态码 | message        | 场景                     |
| ------ | -------------- | ------------------------ |
| 401    | 用户名或密码错误 | 账号或密码不匹配         |
| 403    | 账号已被禁用   | status = 0               |

### 2.3 刷新 Token

`POST /auth/refresh`

**Request Body**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response 200**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 86400
  }
}
```

### 2.4 退出登录

`POST /auth/logout`

**Headers**: Authorization: Bearer \<token\>

**Request Body**（可选）:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response 200**:
```json
{
  "code": 200,
  "message": "success"
}
```

---

## 3. 用户模块

### 3.1 获取当前用户信息

`GET /users/me`

**Response 200**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "email": "zhangsan@example.com",
    "phone": "13800138000",
    "avatar": "https://...",
    "status": 1,
    "lastLoginAt": "2026-05-29T10:30:00",
    "createdAt": "2026-05-01T08:00:00"
  }
}
```

### 3.2 更新当前用户信息

`PATCH /users/me`

**Request Body**:
```json
{
  "nickname": "张三丰",
  "email": "zhangsanfeng@example.com",
  "phone": "13900139000",
  "avatar": "https://..."
}
```

**Response 200**:
```json
{
  "code": 200,
  "message": "更新成功",
  "data": { ... }
}
```

### 3.3 修改密码

`PATCH /users/me/password`

**Request Body**:
```json
{
  "oldPassword": "Abc123456",
  "newPassword": "Def654321"
}
```

**Response 200**:
```json
{
  "code": 200,
  "message": "密码修改成功"
}
```

### 3.4 获取用户列表（管理员）

`GET /users?page=1&size=20&keyword=zhang&status=1`

| 参数    | 类型   | 必填 | 说明                   |
| ------- | ------ | ---- | ---------------------- |
| page    | int    | 否   | 默认 1                 |
| size    | int    | 否   | 默认 20                |
| keyword | string | 否   | 模糊匹配用户名/昵称    |
| status  | int    | 否   | 1=启用 0=禁用          |

**Response 200**（分页响应）:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "username": "zhangsan",
        "nickname": "张三",
        "email": "zhangsan@example.com",
        "phone": "13800138000",
        "status": 1,
        "lastLoginAt": "2026-05-29T10:30:00",
        "createdAt": "2026-05-01T08:00:00"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## 4. 偏好设置模块

### 4.1 获取偏好设置

`GET /preferences`

**Response 200**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "defaultView": "month",
    "defaultCategory": "personal",
    "defaultReminder": 15,
    "language": "zh-CN",
    "weekStartDay": 1,
    "workingHoursStart": "09:00:00",
    "workingHoursEnd": "18:00:00",
    "ttsEnabled": true,
    "ttsVoiceType": "female",
    "ttsSpeed": 5,
    "notificationEnabled": true,
    "theme": "light"
  }
}
```

### 4.2 更新偏好设置

`PATCH /preferences`

| 字段                | 类型    | 约束                 |
| ------------------- | ------- | -------------------- |
| defaultView         | string  | month / week / day   |
| defaultCategory     | string  | personal / work / family / other |
| defaultReminder     | int     | 0-1440（分钟）       |
| language            | string  | zh-CN / en           |
| weekStartDay        | int     | 0（周日）/ 1（周一） |
| workingHoursStart   | string  | HH:mm:ss             |
| workingHoursEnd     | string  | HH:mm:ss             |
| ttsEnabled          | boolean | -                    |
| ttsVoiceType        | string  | female / male / child|
| ttsSpeed            | int     | 0-15                 |
| notificationEnabled | boolean | -                    |
| theme               | string  | light / dark         |

**Request Body**:
```json
{
  "defaultView": "week",
  "ttsEnabled": true,
  "ttsSpeed": 6
}
```

**Response 200**:
```json
{
  "code": 200,
  "message": "更新成功",
  "data": { ... }
}
```

### 4.3 重置偏好设置

`PATCH /preferences/reset`

重置为系统默认值。

**Response 200**:
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

---

## 5. 日历事件模块

### 5.1 创建事件

`POST /events`

**Request Body**:
```json
{
  "title": "项目评审会",
  "description": "季度项目评审",
  "startTime": "2026-06-01T14:00:00",
  "endTime": "2026-06-01T16:00:00",
  "allDay": false,
  "location": "3楼会议室A",
  "category": "work",
  "color": "#409EFF",
  "priority": 8,
  "repeatRule": {
    "ruleType": "weekly",
    "intervalValue": 1,
    "daysOfWeek": "1,3,5",
    "endType": "count",
    "endCount": 10
  },
  "reminders": [
    { "remindMinutesBefore": 30, "method": "browser" },
    { "remindMinutesBefore": 5, "method": "tts" }
  ]
}
```

| 字段          | 类型    | 必填 | 约束                     |
| ------------- | ------- | ---- | ------------------------ |
| title         | string  | 是   | 1-200 字符               |
| description   | string  | 否   |                          |
| startTime     | string  | 是   | ISO 8601 格式            |
| endTime       | string  | 是   | 必须晚于 startTime       |
| allDay        | boolean | 否   | 默认 false               |
| location      | string  | 否   |                          |
| category      | string  | 否   | personal / work / family / other |
| color         | string  | 否   | 十六进制颜色             |
| priority      | int     | 否   | 1-10                     |
| repeatRule    | object  | 否   | 详见第 6 节              |
| reminders     | array   | 否   | 详见第 7 节              |

**Response 201**:
```json
{
  "code": 201,
  "message": "创建成功",
  "data": {
    "id": 42,
    "title": "项目评审会",
    "startTime": "2026-06-01T14:00:00",
    "endTime": "2026-06-01T16:00:00",
    "status": "active",
    "isRecurring": true,
    "createdAt": "2026-05-29T10:00:00"
  }
}
```

### 5.2 获取事件详情

`GET /events/{eventId}`

**Response 200**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 42,
    "userId": 1,
    "title": "项目评审会",
    "description": "季度项目评审",
    "startTime": "2026-06-01T14:00:00",
    "endTime": "2026-06-01T16:00:00",
    "allDay": false,
    "location": "3楼会议室A",
    "category": "work",
    "color": "#409EFF",
    "priority": 8,
    "status": "active",
    "source": "manual",
    "isRecurring": true,
    "parentEventId": null,
    "originalDate": null,
    "repeatRule": {
      "id": 15,
      "ruleType": "weekly",
      "intervalValue": 1,
      "daysOfWeek": "1,3,5",
      "endType": "count",
      "endCount": 10
    },
    "reminders": [
      {
        "id": 30,
        "remindMinutesBefore": 30,
        "method": "browser",
        "status": "pending"
      }
    ],
    "createdAt": "2026-05-29T10:00:00",
    "updatedAt": "2026-05-29T10:00:00"
  }
}
```

### 5.3 查询事件列表

`GET /events?page=1&size=20&startDate=2026-06-01&endDate=2026-06-30&category=work&status=active&keyword=评审`

**查询参数**:

| 参数      | 类型   | 必填 | 说明                           |
| --------- | ------ | ---- | ------------------------------ |
| startDate | string | 否   | 开始日期（含），YYYY-MM-DD     |
| endDate   | string | 否   | 结束日期（含），YYYY-MM-DD     |
| category  | string | 否   | 事件分类                       |
| status    | string | 否   | 状态：active / cancelled / completed |
| priority  | int    | 否   | 最低优先级筛选                 |
| keyword   | string | 否   | 标题/描述模糊搜索              |
| source    | string | 否   | 来源：manual / voice / import  |

**Response 200**（分页响应）:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 42,
        "title": "项目评审会",
        "startTime": "2026-06-01T14:00:00",
        "endTime": "2026-06-01T16:00:00",
        "allDay": false,
        "category": "work",
        "color": "#409EFF",
        "status": "active",
        "isRecurring": true
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### 5.4 获取日历视图数据（含农历）

`GET /events/calendar?view=month&date=2026-06-01`

**查询参数**:

| 参数 | 类型   | 必填 | 说明                               |
| ---- | ------ | ---- | ---------------------------------- |
| view | string | 是   | month / week / day                 |
| date | string | 是   | 视图中心日期，YYYY-MM-DD           |

**Response 200**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "view": "month",
    "currentDate": "2026-06-01",
    "lunarDate": "四月十六",
    "lunarYear": "丙午年",
    "yearZodiac": "马",
    "festivals": [
      {
        "name": "芒种",
        "date": "2026-06-05",
        "type": "solar_term",
        "greeting": "芒种至 🏋️！忙种忙收，不负时光~",
        "suggestions": ["制定下半年的目标吧"],
        "icon": "mangzhong",
        "isLunar": false
      },
      {
        "name": "端午节",
        "date": null,
        "type": "traditional",
        "greeting": "端午临 🎋！粽香满堂，安康过节~",
        "suggestions": ["吃个粽子，甜咸都来一个"],
        "icon": "duanwu",
        "isLunar": true,
        "lunarDate": "五月初五"
      }
    ],
    "days": [
      {
        "date": "2026-06-01",
        "isCurrentMonth": true,
        "isToday": false,
        "lunarDate": "四月十六",
        "festivals": [],
        "events": [
          {
            "id": 42,
            "title": "项目评审会",
            "startTime": "2026-06-01T14:00:00",
            "endTime": "2026-06-01T16:00:00",
            "color": "#409EFF",
            "category": "work"
          }
        ]
      }
    ]
  }
}
```

### 5.5 更新事件

`PUT /events/{eventId}` — 全量更新  
`PATCH /events/{eventId}` — 部分更新

**Request Body**（PATCH 示例）:
```json
{
  "title": "项目评审会（改期）",
  "startTime": "2026-06-02T14:00:00",
  "endTime": "2026-06-02T16:00:00"
}
```

**Response 200**:
```json
{
  "code": 200,
  "message": "更新成功",
  "data": { ... }
}
```

### 5.6 删除事件

`DELETE /events/{eventId}`

**Response 204**: 无响应体

**查询参数**:

| 参数       | 类型    | 必填 | 说明                          |
| ---------- | ------- | ---- | ----------------------------- |
| deleteRule | boolean | 否   | 是否同时删除重复规则，默认 true |

### 5.7 批量删除事件

`POST /events/batch-delete`

**Request Body**:
```json
{
  "ids": [42, 43, 44]
}
```

**Response 200**:
```json
{
  "code": 200,
  "message": "批量删除成功",
  "data": {
    "deletedCount": 3
  }
}
```

### 5.8 更新事件状态

`PATCH /events/{eventId}/status`

**Request Body**:
```json
{
  "status": "completed"
}
```

**Response 200**:
```json
{
  "code": 200,
  "message": "状态更新成功"
}
```

---

## 6. 重复规则模块

### 6.1 创建重复规则

`POST /events/{eventId}/repeat-rule`

**Request Body**:
```json
{
  "ruleType": "weekly",
  "intervalValue": 2,
  "daysOfWeek": "1,3,5",
  "daysOfMonth": null,
  "monthlyType": null,
  "weekOfMonth": null,
  "endType": "date",
  "endCount": null,
  "endDate": "2026-12-31T23:59:59"
}
```

| 字段          | 类型   | 必填 | 说明                                     |
| ------------- | ------ | ---- | ---------------------------------------- |
| ruleType      | string | 是   | daily / weekly / monthly / yearly / custom |
| intervalValue | int    | 否   | 默认 1                                   |
| daysOfWeek    | string | 否   | 逗号分隔，1=周一 … 7=周日                |
| daysOfMonth   | string | 否   | 逗号分隔数字                             |
| monthlyType   | string | 否   | date（按日期）/ weekday（按周几）         |
| weekOfMonth   | int    | 否   | 1-5                                      |
| endType       | string | 否   | never / count / date                     |
| endCount      | int    | 否   | endType=count 时必填                     |
| endDate       | string | 否   | endType=date 时必填                      |

### 6.2 获取重复规则

`GET /events/{eventId}/repeat-rule`

### 6.3 更新重复规则

`PATCH /events/{eventId}/repeat-rule`

### 6.4 删除重复规则

`DELETE /events/{eventId}/repeat-rule`

**Response 204**

### 6.5 生成重复事件实例

`POST /events/{eventId}/repeat-rule/generate`

**查询参数**:

| 参数      | 类型   | 必填 | 说明                        |
| --------- | ------ | ---- | --------------------------- |
| startDate | string | 否   | 生成起始日期，默认当前日期  |
| endDate   | string | 否   | 生成截止日期                |

**Response 200**:
```json
{
  "code": 200,
  "message": "生成成功",
  "data": {
    "generatedCount": 26
  }
}
```

---

## 7. 提醒模块

### 7.1 创建提醒

`POST /events/{eventId}/reminders`

**Request Body**:
```json
{
  "remindMinutesBefore": 15,
  "method": "tts"
}
```

| 字段                | 类型   | 必填 | 约束                       |
| ------------------- | ------ | ---- | -------------------------- |
| remindMinutesBefore | int    | 是   | 0-1440                     |
| method              | string | 是   | browser / tts / email / all |

### 7.2 获取事件的提醒列表

`GET /events/{eventId}/reminders`

### 7.3 更新提醒

`PATCH /reminders/{reminderId}`

### 7.4 删除提醒

`DELETE /reminders/{reminderId}`

### 7.5 获取待发送提醒（内部/定时任务）

`GET /reminders/pending?before=2026-05-29T10:00:00`

**Response 200**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 30,
      "eventId": 42,
      "userId": 1,
      "title": "项目评审会",
      "remindAt": "2026-06-01T13:30:00",
      "method": "tts",
      "minutesBefore": 30
    }
  ]
}
```

### 7.6 标记提醒已发送

`PATCH /reminders/{reminderId}/sent`

**Response 200**:
```json
{
  "code": 200,
  "message": "success"
}
```

---

## 8. 节日模块

### 8.1 获取指定日期的节日

`GET /festivals?date=2026-06-05`

**Response 200**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 32,
      "name": "芒种",
      "date": "2026-06-05",
      "type": "solar_term",
      "description": "二十四节气之芒种",
      "greeting": "芒种至 🏋️！忙种忙收，不负时光~",
      "suggestions": ["制定下半年的目标吧"],
      "icon": "mangzhong"
    }
  ]
}
```

### 8.2 获取当月/指定月份节日

`GET /festivals/month?year=2026&month=6`

**Response 200**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "name": "芒种",
      "date": "2026-06-05",
      "type": "solar_term",
      "greeting": "芒种至 🏋️！忙种忙收，不负时光~",
      "icon": "mangzhong"
    },
    {
      "name": "端午节",
      "date": null,
      "type": "traditional",
      "isLunar": true,
      "lunarDate": "五月初五",
      "greeting": "端午临 🎋！粽香满堂，安康过节~",
      "icon": "duanwu"
    },
    {
      "name": "夏至",
      "date": "2026-06-21",
      "type": "solar_term",
      "greeting": "夏至临 ☀️！日最长，影最短~",
      "icon": "xiazhi"
    }
  ]
}
```

### 8.3 获取今日节日（含农历推算）

`GET /festivals/today`

**Response 200**（无节日时 data 为 null）:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "festival": {
      "name": "端午节",
      "type": "traditional",
      "greeting": "端午临 🎋！粽香满堂，安康过节~",
      "suggestions": ["吃个粽子，甜咸都来一个"],
      "icon": "duanwu"
    },
    "lunarDate": "五月初五",
    "daysUntilNext": 7
  }
}
```

### 8.4 获取年度节日一览

`GET /festivals/year?year=2026`

---

## 9. 语音命令模块

### 9.1 WebSocket 语音命令入口

`WS /ws/voice?token=<jwt_token>`

**消息格式（客户端 → 服务端）**:
```json
{
  "type": "audio_data",
  "sessionId": "uuid",
  "payload": {
    "audioBase64": "//uQxAAAAAANIAAAAAE...",
    "format": "pcm",
    "sampleRate": 16000
  }
}
```

**消息格式（服务端 → 客户端）**:
```json
{
  "type": "intermediate_result",
  "sessionId": "uuid",
  "payload": {
    "text": "下周...",
    "isFinal": false
  }
}
```

```json
{
  "type": "final_result",
  "sessionId": "uuid",
  "payload": {
    "text": "下周三下午三点创建项目评审会",
    "intent": "CREATE",
    "entities": {
      "date": "2026-06-03",
      "time": "15:00",
      "title": "项目评审会"
    },
    "confidence": 0.95,
    "action": {
      "type": "preview",
      "event": {
        "title": "项目评审会",
        "startTime": "2026-06-03T15:00:00",
        "endTime": "2026-06-03T16:00:00"
      }
    },
    "requiresClarify": false
  }
}
```

```json
{
  "type": "clarify_request",
  "sessionId": "uuid",
  "payload": {
    "question": "请问会议持续多久？",
    "missingFields": ["endTime"]
  }
}
```

### 9.2 查询语音命令日志

`GET /voice-logs?page=1&size=20&intent=CREATE&result=failed`

**Response 200**（分页响应）:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "rawAudioText": "下周三下午三点创建项目评审会",
        "intent": "CREATE",
        "entities": { "date": "2026-06-03", "time": "15:00" },
        "confidence": 0.95,
        "nluSource": "llm",
        "commandResult": "success",
        "responseText": "好的，已为您创建事件「项目评审会」",
        "durationMs": 1234,
        "createdAt": "2026-05-29T10:00:00"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1
  }
}
```

---

## 10. 语音服务模块（Baidu Cloud）

### 10.1 TTS 文本转语音

`POST /voice/tts`

**Request Body**:
```json
{
  "text": "端午临，粽香满堂，安康过节~",
  "voiceType": "female",
  "speed": 5,
  "pitch": 5
}
```

| 字段      | 类型   | 必填 | 说明                               |
| --------- | ------ | ---- | ---------------------------------- |
| text      | string | 是   | 文本，建议不超过 200 字             |
| voiceType | string | 否   | female / male / child，默认 female |
| speed     | int    | 否   | 0-15，默认 5                       |
| pitch     | int    | 否   | 0-15，默认 5                       |

**Response 200**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "audioBase64": "//uQxAAAAAANIAAAAAE...",
    "format": "mp3",
    "durationMs": 3200,
    "textLength": 18
  }
}
```

**错误码**:
| 状态码 | message        | 场景               |
| ------ | -------------- | ------------------ |
| 422    | 文本超长       | 超过 500 字符      |
| 500    | TTS 服务调用失败 | Baidu API 异常     |

### 10.2 ASR 语音转文本

`POST /voice/asr`

**Request Body**:
```json
{
  "audioBase64": "//uQxAAAAAANIAAAAAE...",
  "format": "pcm",
  "sampleRate": 16000
}
```

| 字段        | 类型   | 必填 | 说明                     |
| ----------- | ------ | ---- | ------------------------ |
| audioBase64 | string | 是   | Base64 编码的音频数据    |
| format      | string | 是   | pcm / wav / mp3 / amr    |
| sampleRate  | int    | 否   | 16000 / 8000 等          |

**Response 200**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "text": "下周三下午三点创建项目评审会",
    "confidence": 0.95,
    "durationMs": 2300
  }
}
```

### 10.3 节日关怀 TTS 播报

`POST /voice/festival-greeting`

触发自动 TTS 播报当日节日祝福（供前端调用，或后端定时推送）。

**Request Body**:
```json
{
  "festivalId": 5,
  "userId": 1,
  "eventId": null
}
```

**Response 200**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "greetingText": "端午临 🎋！粽香满堂，安康过节~ 建议：吃个粽子，甜咸都来一个",
    "audioBase64": "//uQxAAAAAANIAAAAAE...",
    "durationMs": 4500
  }
}
```

---

## 11. 审计日志模块

### 11.1 查询审计日志（管理员）

`GET /audit-logs?page=1&size=20&userId=1&action=CREATE&entityType=event&startDate=2026-05-01&endDate=2026-05-31`

**Response 200**（分页响应）:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 100,
        "userId": 1,
        "action": "CREATE",
        "entityType": "event",
        "entityId": 42,
        "details": {
          "title": "项目评审会",
          "category": "work"
        },
        "ipAddress": "192.168.1.100",
        "userAgent": "Mozilla/5.0...",
        "createdAt": "2026-05-29T10:00:00"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 50,
    "totalPages": 3
  }
}
```

---

## 12. 附录

### 12.1 枚举值说明

**事件分类 `category`**:
| 值       | 说明   |
| -------- | ------ |
| personal | 个人   |
| work     | 工作   |
| family   | 家庭   |
| other    | 其他   |

**事件状态 `status`**:
| 值        | 说明   |
| --------- | ------ |
| active    | 正常   |
| cancelled | 已取消 |
| completed | 已完成 |

**事件来源 `source`**:
| 值     | 说明       |
| ------ | ---------- |
| manual | 手动创建   |
| voice  | 语音创建   |
| import | 批量导入   |

**节日类型 `type`**:
| 值          | 说明       |
| ----------- | ---------- |
| traditional | 传统节日   |
| western     | 西方节日   |
| solar_term  | 二十四节气 |
| other       | 其他节日   |

**重复类型 `ruleType`**:
| 值     | 说明   |
| ------ | ------ |
| daily  | 每天   |
| weekly | 每周   |
| monthly | 每月  |
| yearly | 每年   |
| custom | 自定义 |

**结束类型 `endType`**:
| 值    | 说明     |
| ----- | -------- |
| never | 永不结束 |
| count | 按次数   |
| date  | 按日期   |

**提醒方式 `method`**:
| 值      | 说明     |
| ------- | -------- |
| browser | 浏览器通知 |
| tts     | 语音播报 |
| email   | 邮件通知 |
| all     | 全部方式 |

**语音命令意图 `intent`**:
| 值       | 说明     |
| -------- | -------- |
| CREATE   | 创建事件 |
| QUERY    | 查询事件 |
| UPDATE   | 修改事件 |
| DELETE   | 删除事件 |
| REMINDER | 设置提醒 |

**操作类型 `action`**:
| 值     | 说明     |
| ------ | -------- |
| CREATE | 创建     |
| UPDATE | 更新     |
| DELETE | 删除     |
| LOGIN  | 登录     |
| LOGOUT | 退出     |

### 12.2 全局错误码

| code  | message            | 说明               |
| ----- | ------------------ | ------------------ |
| 1001  | 认证失败           | Token 无效或过期   |
| 1002  | 无权限访问         | 权限不足           |
| 1003  | 请求频率超限       | 请稍后再试         |
| 2001  | 资源不存在         | 请求的资源未找到   |
| 2002  | 参数校验失败       | 请求参数不合法     |
| 2003  | 资源冲突           | 唯一约束冲突       |
| 3001  | 内部服务器错误     | 系统异常           |
| 3002  | 外部服务调用失败   | Baidu Cloud API 异常 |

### 12.3 接口路径汇总

| HTTP 方法 | 路径                                          | 说明               |
| --------- | --------------------------------------------- | ------------------ |
| POST      | /auth/register                                | 用户注册           |
| POST      | /auth/login                                   | 用户登录           |
| POST      | /auth/refresh                                 | 刷新 Token         |
| POST      | /auth/logout                                  | 退出登录           |
| GET       | /users/me                                     | 获取当前用户       |
| PATCH     | /users/me                                     | 更新当前用户       |
| PATCH     | /users/me/password                            | 修改密码           |
| GET       | /preferences                                  | 获取偏好设置       |
| PATCH     | /preferences                                  | 更新偏好设置       |
| POST      | /events                                       | 创建事件           |
| GET       | /events                                       | 查询事件列表       |
| GET       | /events/calendar                              | 日历视图数据       |
| GET       | /events/{eventId}                             | 事件详情           |
| PUT       | /events/{eventId}                             | 全量更新事件       |
| PATCH     | /events/{eventId}                             | 部分更新事件       |
| DELETE    | /events/{eventId}                             | 删除事件           |
| POST      | /events/batch-delete                          | 批量删除事件       |
| PATCH     | /events/{eventId}/status                      | 更新事件状态       |
| POST      | /events/{eventId}/repeat-rule                 | 创建重复规则       |
| GET       | /events/{eventId}/repeat-rule                 | 获取重复规则       |
| PATCH     | /events/{eventId}/repeat-rule                 | 更新重复规则       |
| DELETE    | /events/{eventId}/repeat-rule                 | 删除重复规则       |
| POST      | /events/{eventId}/repeat-rule/generate        | 生成重复实例       |
| POST      | /events/{eventId}/reminders                   | 创建提醒           |
| GET       | /events/{eventId}/reminders                   | 提醒列表           |
| PATCH     | /reminders/{reminderId}                       | 更新提醒           |
| DELETE    | /reminders/{reminderId}                       | 删除提醒           |
| GET       | /reminders/pending                            | 待发送提醒         |
| PATCH     | /reminders/{reminderId}/sent                  | 标记已发送         |
| GET       | /festivals                                    | 指定日期节日       |
| GET       | /festivals/today                              | 今日节日           |
| GET       | /festivals/month                              | 当月节日           |
| GET       | /festivals/year                               | 年度节日           |
| WS        | /ws/voice                                     | 语音命令 WebSocket |
| GET       | /voice-logs                                   | 语音命令日志       |
| POST      | /voice/tts                                    | 文本转语音         |
| POST      | /voice/asr                                    | 语音转文本         |
| POST      | /voice/festival-greeting                      | 节日关怀播报       |
| GET       | /audit-logs                                   | 审计日志           |
| POST      | /auth/refresh                                 | 刷新 Token         |
| POST      | /auth/logout                                  | 退出登录           |
| GET       | /preferences                                  | 获取偏好设置       |
| PATCH     | /preferences                                  | 更新偏好设置       |
| PATCH     | /preferences/reset                            | 重置偏好设置       |
