package com.voicecal.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResult<T> {

    private int code;
    private String message;
    private T data;
    private PageInfo page;
    private List<FieldError> errors;
    private long timestamp;

    private ApiResult() {
        this.timestamp = Instant.now().toEpochMilli();
    }

    // ========== 成功 ==========

    public static <T> ApiResult<T> success() {
        return success(null);
    }

    public static <T> ApiResult<T> success(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.code = ResultCode.SUCCESS.getCode();
        result.message = ResultCode.SUCCESS.getMessage();
        result.data = data;
        return result;
    }

    public static <T> ApiResult<T> created(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.code = ResultCode.CREATED.getCode();
        result.message = ResultCode.CREATED.getMessage();
        result.data = data;
        return result;
    }

    public static <T> ApiResult<T> noContent() {
        ApiResult<T> result = new ApiResult<>();
        result.code = ResultCode.NO_CONTENT.getCode();
        result.message = ResultCode.NO_CONTENT.getMessage();
        return result;
    }

    // ========== 分页 ==========

    public static <T> ApiResult<List<T>> page(List<T> content, long totalElements, int pageNum, int pageSize) {
        ApiResult<List<T>> result = new ApiResult<>();
        result.code = ResultCode.SUCCESS.getCode();
        result.message = ResultCode.SUCCESS.getMessage();
        result.data = content;
        result.page = new PageInfo(pageNum, pageSize, totalElements);
        return result;
    }

    // ========== 错误 ==========

    public static <T> ApiResult<T> error(ResultCode resultCode) {
        return error(resultCode.getCode(), resultCode.getMessage());
    }

    public static <T> ApiResult<T> error(ResultCode resultCode, String customMessage) {
        ApiResult<T> result = new ApiResult<>();
        result.code = resultCode.getCode();
        result.message = customMessage;
        return result;
    }

    public static <T> ApiResult<T> error(int code, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.code = code;
        result.message = message;
        return result;
    }

    public static <T> ApiResult<T> error(int code, String message, List<FieldError> errors) {
        ApiResult<T> result = new ApiResult<>();
        result.code = code;
        result.message = message;
        result.errors = errors;
        return result;
    }

    public static <T> ApiResult<T> badRequest(String message) {
        return error(ResultCode.BAD_REQUEST.getCode(), message);
    }

    public static <T> ApiResult<T> badRequest(List<FieldError> errors) {
        return error(ResultCode.PARAM_VALIDATION_FAILED.getCode(),
                ResultCode.PARAM_VALIDATION_FAILED.getMessage(), errors);
    }

    public static <T> ApiResult<T> unauthorized(String message) {
        return error(ResultCode.UNAUTHORIZED.getCode(), message);
    }

    public static <T> ApiResult<T> forbidden(String message) {
        return error(ResultCode.FORBIDDEN.getCode(), message);
    }

    public static <T> ApiResult<T> notFound(String message) {
        return error(ResultCode.NOT_FOUND.getCode(), message);
    }

    public static <T> ApiResult<T> conflict(String message) {
        return error(ResultCode.CONFLICT.getCode(), message);
    }

    // ========== 便捷判断 ==========

    public boolean isSuccess() {
        return code >= 200 && code < 300;
    }

    public boolean isError() {
        return !isSuccess();
    }

    // ========== Getter / Setter ==========

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public PageInfo getPage() {
        return page;
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // ========== 内部类 ==========

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PageInfo {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;

        public PageInfo() {}

        public PageInfo(int page, int size, long totalElements) {
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = (int) Math.ceil((double) totalElements / size);
        }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldError {
        private String field;
        private String message;

        public FieldError() {}

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
