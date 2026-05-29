-- ============================================================
-- VoiceCal 智能语音日历 - 数据库建表脚本
-- 数据库: voice_calendar
-- MySQL: 8.0.42
-- 字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS `voice_calendar`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `voice_calendar`;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE `user` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT,
  `username`        VARCHAR(50)   NOT NULL COMMENT '用户名',
  `password`        VARCHAR(255)  NOT NULL COMMENT 'BCrypt 加密密码',
  `nickname`        VARCHAR(50)            COMMENT '昵称',
  `email`           VARCHAR(100)           COMMENT '邮箱',
  `phone`           VARCHAR(20)            COMMENT '手机号',
  `avatar`          VARCHAR(500)           COMMENT '头像 URL',
  `status`          TINYINT       DEFAULT 1 COMMENT '状态 1:正常 0:禁用',
  `last_login_at`   DATETIME               COMMENT '最后登录时间',
  `created_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. 用户偏好设置表
-- ============================================================
CREATE TABLE `user_preference` (
  `id`                  BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`             BIGINT        NOT NULL COMMENT '用户 ID',
  `default_view`        VARCHAR(20)   DEFAULT 'month' COMMENT '默认日历视图 month/week/day',
  `default_category`    VARCHAR(50)   DEFAULT 'personal' COMMENT '默认事件分类',
  `default_reminder`    INT           DEFAULT 15 COMMENT '默认提前提醒分钟数',
  `language`            VARCHAR(10)   DEFAULT 'zh-CN' COMMENT '语言',
  `week_start_day`      TINYINT       DEFAULT 1 COMMENT '每周起始日 0:周日 1:周一',
  `working_hours_start` TIME          DEFAULT '09:00:00' COMMENT '工作时间开始',
  `working_hours_end`   TIME          DEFAULT '18:00:00' COMMENT '工作时间结束',
  `tts_enabled`         TINYINT(1)    DEFAULT 1 COMMENT '开启语音播报',
  `tts_voice_type`      VARCHAR(20)   DEFAULT 'female' COMMENT '语音音色 female/male/child',
  `tts_speed`           TINYINT       DEFAULT 5 COMMENT '语速 0-15',
  `notification_enabled` TINYINT(1)   DEFAULT 1 COMMENT '开启浏览器通知',
  `theme`               VARCHAR(20)   DEFAULT 'light' COMMENT '主题 light/dark',
  `created_at`          DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  CONSTRAINT `fk_pref_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好设置表';

-- ============================================================
-- 3. 日历事件表（核心表）
-- ============================================================
CREATE TABLE `calendar_event` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`          BIGINT        NOT NULL COMMENT '所属用户',
  `title`            VARCHAR(200)  NOT NULL COMMENT '事件标题',
  `description`      TEXT                   COMMENT '事件描述',
  `start_time`       DATETIME      NOT NULL COMMENT '开始时间',
  `end_time`         DATETIME      NOT NULL COMMENT '结束时间',
  `all_day`          TINYINT(1)    DEFAULT 0 COMMENT '是否全天事件 0:否 1:是',
  `location`         VARCHAR(200)           COMMENT '地点',
  `category`         VARCHAR(50)   DEFAULT 'personal' COMMENT '分类 personal/work/family/other',
  `color`            VARCHAR(20)   DEFAULT '#409EFF' COMMENT '日历显示颜色',
  `priority`         TINYINT       DEFAULT 5 COMMENT '优先级 1-10(10最高)',
  `status`           VARCHAR(20)   DEFAULT 'active' COMMENT '状态 active/cancelled/completed',
  `source`           VARCHAR(20)   DEFAULT 'manual' COMMENT '来源 manual/voice/import',
  `is_recurring`     TINYINT(1)    DEFAULT 0 COMMENT '是否重复事件',
  `parent_event_id`  BIGINT                 COMMENT '重复主事件ID(子实例指向父事件)',
  `original_date`    DATE                   COMMENT '原始日期(重复实例的原始日期)',
  `created_at`       DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `start_time`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_category` (`user_id`, `category`),
  CONSTRAINT `fk_event_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日历事件表';

-- ============================================================
-- 4. 重复规则表
-- ============================================================
CREATE TABLE `repeat_rule` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT,
  `event_id`        BIGINT        NOT NULL COMMENT '关联事件 ID',
  `rule_type`       VARCHAR(20)   NOT NULL COMMENT '重复类型 daily/weekly/monthly/yearly/custom',
  `interval_value`  INT           DEFAULT 1 COMMENT '间隔数(如每2天/每3周)',
  `days_of_week`    VARCHAR(50)            COMMENT '周重复日(如"1,3,5"表示周一三五)',
  `days_of_month`   VARCHAR(100)           COMMENT '月重复日(如"15,30")',
  `monthly_type`    VARCHAR(20)            COMMENT '月重复类型 date:按日期 weekday:按周几',
  `week_of_month`   TINYINT                COMMENT '月第几周(1-5)',
  `end_type`        VARCHAR(20)   DEFAULT 'never' COMMENT '结束类型 never/count/date',
  `end_count`       INT                     COMMENT '重复次数',
  `end_date`        DATETIME               COMMENT '结束日期',
  `created_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_event_id` (`event_id`),
  CONSTRAINT `fk_repeat_event` FOREIGN KEY (`event_id`) REFERENCES `calendar_event` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='重复规则表';

-- ============================================================
-- 5. 提醒表
-- ============================================================
CREATE TABLE `reminder` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT,
  `event_id`        BIGINT        NOT NULL COMMENT '关联事件 ID',
  `user_id`         BIGINT        NOT NULL COMMENT '所属用户',
  `remind_at`       DATETIME      NOT NULL COMMENT '提醒触发时间',
  `remind_minutes_before` INT     DEFAULT 15 COMMENT '提前分钟数',
  `method`          VARCHAR(50)   DEFAULT 'browser' COMMENT '提醒方式 browser/tts/email/all',
  `is_sent`         TINYINT(1)    DEFAULT 0 COMMENT '是否已发送',
  `sent_at`         DATETIME               COMMENT '实际发送时间',
  `status`          VARCHAR(20)   DEFAULT 'pending' COMMENT '状态 pending/sent/cancelled',
  `created_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_remind_at` (`remind_at`, `is_sent`),
  KEY `idx_event_id` (`event_id`),
  KEY `idx_user_remind` (`user_id`, `remind_at`),
  CONSTRAINT `fk_remind_event` FOREIGN KEY (`event_id`) REFERENCES `calendar_event` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_remind_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提醒表';

-- ============================================================
-- 6. 语音命令日志表
-- ============================================================
CREATE TABLE `voice_command_log` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT        NOT NULL COMMENT '用户 ID',
  `session_id`      VARCHAR(100)           COMMENT 'WebSocket 会话 ID',
  `raw_audio_text`  TEXT                   COMMENT 'ASR 识别文本',
  `intent`          VARCHAR(50)            COMMENT '意图 CREATE/QUERY/UPDATE/DELETE/REMINDER',
  `entities`        JSON                   COMMENT '提取的实体(JSON)',
  `confidence`      DECIMAL(3,2)           COMMENT '识别置信度',
  `nlu_source`      VARCHAR(20)            COMMENT 'NLU来源 llm/rule/clarify',
  `command_result`  VARCHAR(20)            COMMENT '处理结果 success/failed/partial',
  `response_text`   TEXT                   COMMENT '系统回复文本',
  `duration_ms`     INT                    COMMENT '处理耗时(毫秒)',
  `error_message`   VARCHAR(500)           COMMENT '错误信息',
  `created_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_session` (`user_id`, `session_id`),
  KEY `idx_user_time` (`user_id`, `created_at`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_vlog_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语音命令日志表';

-- ============================================================
-- 7. 节日表
-- ============================================================
CREATE TABLE `festival` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT,
  `name`            VARCHAR(100)  NOT NULL COMMENT '节日名称',
  `date`            DATE                   COMMENT '节日日期(公历节日固定日期；农历节日为NULL，由 lunar_date 动态计算)',
  `type`            VARCHAR(30)   NOT NULL COMMENT '节日类型 traditional/western/solar_term/other',
  `description`     VARCHAR(500)           COMMENT '节日描述',
  `greeting`        VARCHAR(500)           COMMENT '祝福语',
  `suggestions`     JSON                   COMMENT '建议动作(JSON数组)',
  `icon`            VARCHAR(50)            COMMENT '图标标识',
  `is_lunar`        TINYINT(1)    DEFAULT 0 COMMENT '是否农历节日',
  `lunar_date`      VARCHAR(20)            COMMENT '农历日期(如"五月初五")',
  `created_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_date` (`date`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节日表';

-- ============================================================
-- 8. 节日关怀日志表
-- ============================================================
CREATE TABLE `festival_greeting_log` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT        NOT NULL COMMENT '用户 ID',
  `festival_id`     BIGINT        NOT NULL COMMENT '节日 ID',
  `event_id`        BIGINT                 COMMENT '触发事件 ID',
  `greeting_type`   VARCHAR(30)            COMMENT '关怀类型 first_create/suggestion',
  `greeting_text`   VARCHAR(1000)          COMMENT '发送的祝福文本',
  `is_tts_sent`     TINYINT(1)    DEFAULT 0 COMMENT '是否已 TTS 播报',
  `user_action`     VARCHAR(30)            COMMENT '用户操作 accept/dismiss/snooze',
  `created_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_festival` (`user_id`, `festival_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_greet_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_greet_festival` FOREIGN KEY (`festival_id`) REFERENCES `festival` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节日关怀日志表';

-- ============================================================
-- 9. 操作审计日志表
-- ============================================================
CREATE TABLE `audit_log` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT                 COMMENT '用户 ID',
  `action`          VARCHAR(50)   NOT NULL COMMENT '操作类型 CREATE/UPDATE/DELETE/LOGIN/LOGOUT',
  `entity_type`     VARCHAR(50)            COMMENT '操作对象类型 event/user/preference',
  `entity_id`       BIGINT                 COMMENT '操作对象 ID',
  `details`         JSON                   COMMENT '操作详情(JSON)',
  `ip_address`      VARCHAR(50)            COMMENT '请求 IP',
  `user_agent`      VARCHAR(500)           COMMENT '浏览器 UA',
  `created_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_action` (`user_id`, `action`),
  KEY `idx_entity` (`entity_type`, `entity_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志表';
