package com.plating.erp.common.api;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fe = ex.getBindingResult().getFieldError();
        String msg = fe == null ? "参数校验失败" : fe.getDefaultMessage();
        return new ApiResponse<>(ErrorCode.BAD_REQUEST.code(), msg, null, "trace-" + System.currentTimeMillis(), System.currentTimeMillis());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleConstraint(ConstraintViolationException ex) {
        return new ApiResponse<>(ErrorCode.BAD_REQUEST.code(), ex.getMessage(), null, "trace-" + System.currentTimeMillis(), System.currentTimeMillis());
    }

    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<?> handleBiz(BizException ex) {
        return new ApiResponse<>(ex.getCode(), ex.getMessage(), null, "trace-" + System.currentTimeMillis(), System.currentTimeMillis());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleOther(Exception ex) {
        return new ApiResponse<>(ErrorCode.INTERNAL_ERROR.code(), ex.getMessage(), null, "trace-" + System.currentTimeMillis(), System.currentTimeMillis());
    }
}
