package com.voicecal.common;

public enum ResultCode {

    // ========== 成功 ==========
    SUCCESS(200, "success"),
    CREATED(201, "创建成功"),
    NO_CONTENT(204, "删除成功"),

    // ========== 客户端错误 ==========
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "认证失败，请重新登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "资源冲突"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型"),
    UNPROCESSABLE_ENTITY(422, "业务逻辑错误"),
    TOO_MANY_REQUESTS(429, "请求频率超限，请稍后再试"),

    // ========== 认证与授权 ==========
    TOKEN_INVALID(1001, "Token 无效或已过期"),
    TOKEN_MISSING(1002, "缺少认证 Token"),
    ACCESS_DENIED(1003, "权限不足"),
    ACCOUNT_DISABLED(1004, "账号已被禁用"),
    ACCOUNT_LOCKED(1005, "账号已被锁定"),

    // ========== 资源错误 ==========
    RESOURCE_NOT_FOUND(2001, "请求的资源不存在"),
    RESOURCE_ALREADY_EXISTS(2002, "资源已存在"),
    PARAM_VALIDATION_FAILED(2003, "参数校验失败"),
    PARAM_MISSING(2004, "缺少必填参数"),
    DUPLICATE_KEY(2005, "数据重复，请检查唯一约束"),

    // ========== 服务器错误 ==========
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),
    GATEWAY_TIMEOUT(504, "服务超时"),
    EXTERNAL_SERVICE_ERROR(3001, "外部服务调用失败"),
    DATABASE_ERROR(3002, "数据库操作异常"),
    CACHE_ERROR(3003, "缓存服务异常"),

    // ========== 文件操作 ==========
    FILE_TOO_LARGE(4001, "文件大小超出限制"),
    FILE_TYPE_NOT_ALLOWED(4002, "不支持的文件类型"),
    FILE_UPLOAD_FAILED(4003, "文件上传失败"),

    // ========== 语音服务 ==========
    TTS_SERVICE_ERROR(5001, "TTS 语音合成失败"),
    ASR_SERVICE_ERROR(5002, "ASR 语音识别失败"),
    TEXT_TOO_LONG(5003, "文本超长"),
    AUDIO_FORMAT_UNSUPPORTED(5004, "不支持的音频格式"),

    // ========== 业务逻辑 ==========
    EVENT_TIME_CONFLICT(6001, "事件时间冲突"),
    EVENT_PAST_CANNOT_MODIFY(6002, "已过事件无法修改"),
    REMINDER_ALREADY_SENT(6003, "提醒已发送，无法修改");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static ResultCode fromCode(int code) {
        for (ResultCode rc : values()) {
            if (rc.code == code) {
                return rc;
            }
        }
        return INTERNAL_SERVER_ERROR;
    }
}
