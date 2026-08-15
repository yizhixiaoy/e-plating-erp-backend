package com.plating.erp.audit.vo;

public class LogVo {
    public record ExportReq(String moduleTitle, Integer status, Long userId, String beginTime, String endTime) {
    }
}
