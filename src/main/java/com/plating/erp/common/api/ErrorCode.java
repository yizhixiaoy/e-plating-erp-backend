package com.plating.erp.common.api;

public enum ErrorCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未认证或Token过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_ERROR(500, "系统异常"),
    TENANT_EXPIRED(1001, "租户已过期"),
    TENANT_FROZEN(1002, "租户已冻结"),
    USER_DISABLED(1003, "账号已停用"),
    CROSS_TENANT_FORBIDDEN(1004, "禁止访问其他租户数据"),
    REFRESH_TOKEN_INVALID(1005, "refreshToken无效或已失效");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int code() {
        return code;
    }

    public String msg() {
        return msg;
    }
}
