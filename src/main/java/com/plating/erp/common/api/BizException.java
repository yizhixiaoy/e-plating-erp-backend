package com.plating.erp.common.api;

public class BizException extends RuntimeException {
    private final int code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.msg());
        this.code = errorCode.code();
    }

    public BizException(ErrorCode errorCode, String customMsg) {
        super(customMsg);
        this.code = errorCode.code();
    }

    public int getCode() {
        return code;
    }
}
