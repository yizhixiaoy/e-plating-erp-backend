package com.plating.erp.platform.vo;

/**
 * 租户下拉选项 VO
 * 
 * 用于用户表单中选择租户公司
 */
public record TenantOptionsVo(
        Long id,
        String tenantName,
        String shortCode
) {
}
