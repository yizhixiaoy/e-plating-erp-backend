package com.plating.erp.common.api;

public record ApiResponse<T>(
        int code,
        String msg,
        T data,
        String traceId,
        long timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "操作成功", data, "trace-" + System.currentTimeMillis(), System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> error(int code, String msg) {
        return new ApiResponse<>(code, msg, null, "trace-" + System.currentTimeMillis(), System.currentTimeMillis());
    }
}
