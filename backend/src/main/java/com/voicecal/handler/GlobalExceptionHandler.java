package com.voicecal.handler;

import com.voicecal.common.ApiResult;
import com.voicecal.common.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleValidation(MethodArgumentNotValidException e) {
        List<ApiResult.FieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiResult.FieldError(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());
        return ApiResult.badRequest(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleConstraintViolation(ConstraintViolationException e) {
        List<ApiResult.FieldError> errors = e.getConstraintViolations().stream()
                .map(cv -> new ApiResult.FieldError(
                        cv.getPropertyPath().toString(), cv.getMessage()))
                .collect(Collectors.toList());
        return ApiResult.badRequest(errors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return ApiResult.badRequest("缺少必填参数: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ApiResult.badRequest("参数类型错误: " + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return ApiResult.badRequest("请求体格式错误");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResult<Void> handleDuplicateKey(DuplicateKeyException e) {
        return ApiResult.error(ResultCode.DUPLICATE_KEY);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiResult<Void> handleDataIntegrity(DataIntegrityViolationException e) {
        return ApiResult.error(ResultCode.UNPROCESSABLE_ENTITY.getCode(), "数据完整性冲突");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResult<Void> handleNotFound(NoResourceFoundException e) {
        return ApiResult.error(ResultCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleIllegalArgument(IllegalArgumentException e) {
        return ApiResult.badRequest(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiResult<Void> handleIllegalState(IllegalStateException e) {
        return ApiResult.unprocessableEntity(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleUnhandled(Exception e) {
        log.error("未捕获异常: ", e);
        return ApiResult.error(ResultCode.INTERNAL_SERVER_ERROR);
    }

}
