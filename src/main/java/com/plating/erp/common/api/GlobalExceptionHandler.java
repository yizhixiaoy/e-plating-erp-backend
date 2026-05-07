package com.plating.erp.common.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
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
     * 处理数据库唯一键冲突异常
     * 将数据库层的DuplicateKeyException翻译为友好的用户提示
     * @param ex 唯一键冲突异常
     * @return 包含友好错误信息的API响应
     */
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<?> handleDuplicateKey(DuplicateKeyException ex) {
        log.warn("数据重复异常, message={}", ex.getMessage());
        String msg = friendlyDuplicateMessage(ex.getMessage());
        return new ApiResponse<>(ErrorCode.CONFLICT.code(), msg, null, "trace-" + System.currentTimeMillis(), System.currentTimeMillis());
    }

    /**
     * 根据MySQL唯一键约束名映射友好提示
     */
    private String friendlyDuplicateMessage(String errorMsg) {
        if (errorMsg == null) {
            return "数据已存在，请勿重复添加";
        }
        if (errorMsg.contains("uk_tenant_dept_position")) {
            return "该部门下已存在同名岗位，请勿重复添加";
        }
        if (errorMsg.contains("uk_tenant_name")) {
            return "该公司名称已存在，请勿重复添加";
        }
        if (errorMsg.contains("uk_tenant_username")) {
            return "该用户名已存在，请勿重复添加";
        }
        if (errorMsg.contains("uk_tenant_parent_name")) {
            return "该部门名称已存在，请勿重复添加";
        }
        if (errorMsg.contains("uk_tenant_role_name")) {
            return "该角色名称已存在，请勿重复添加";
        }
        if (errorMsg.contains("uk_short_code")) {
            return "该公司简称已存在，请勿重复添加";
        }
        if (errorMsg.contains("uk_phone") && errorMsg.contains("tenant")) {
            return "该公司联系电话已存在，请勿重复添加";
        }
        if (errorMsg.contains("uk_tenant_realname_phone")) {
            return "该公司已存在同名同手机号的用户，请勿重复添加";
        }
        return "数据已存在，请勿重复添加";
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
