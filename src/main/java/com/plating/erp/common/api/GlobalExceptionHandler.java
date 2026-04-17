package com.plating.erp.common.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolationException;

/**
 * 全局异常处理器
 * 统一处理系统中抛出的各类异常，转换为标准API响应格式
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理参数校验异常（@Valid注解校验失败）
     * @param ex 参数校验异常
     * @return 包含校验错误信息的API响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fe = ex.getBindingResult().getFieldError();
        String msg = fe == null ? "参数校验失败" : fe.getDefaultMessage();
        String field = fe == null ? "unknown" : fe.getField();
        log.warn("参数校验失败, field={}, message={}", field, msg);
        return new ApiResponse<>(ErrorCode.BAD_REQUEST.code(), msg, null, "trace-" + System.currentTimeMillis(), System.currentTimeMillis());
    }

    /**
     * 处理约束校验异常（@RequestParam等校验失败）
     * @param ex 约束校验异常
     * @return 包含错误信息的API响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleConstraint(ConstraintViolationException ex) {
        log.warn("约束校验失败, message={}", ex.getMessage());
        return new ApiResponse<>(ErrorCode.BAD_REQUEST.code(), ex.getMessage(), null, "trace-" + System.currentTimeMillis(), System.currentTimeMillis());
    }

    /**
     * 处理业务异常
     * 业务异常是预期的异常情况，如用户不存在、权限不足等
     * @param ex 业务异常
     * @return 包含业务错误信息的API响应
     */
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<?> handleBiz(BizException ex) {
        // 业务异常分为不同级别记录日志
        if (ex.getCode() >= 500) {
            log.error("业务异常, code={}, message={}", ex.getCode(), ex.getMessage());
        } else {
            log.warn("业务异常, code={}, message={}", ex.getCode(), ex.getMessage());
        }
        return new ApiResponse<>(ex.getCode(), ex.getMessage(), null, "trace-" + System.currentTimeMillis(), System.currentTimeMillis());
    }

    /**
     * 处理未捕获的其他异常
     * 这类异常通常是系统错误或编程错误，需要记录详细日志
     * @param ex 异常
     * @return 包含错误信息的API响应（不暴露详细错误给客户端）
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleOther(Exception ex) {
        log.error("系统异常, message={}", ex.getMessage(), ex);
        // 生产环境不返回详细错误信息给客户端
        String msg = "系统繁忙，请稍后重试";
        return new ApiResponse<>(ErrorCode.INTERNAL_ERROR.code(), msg, null, "trace-" + System.currentTimeMillis(), System.currentTimeMillis());
    }
}
