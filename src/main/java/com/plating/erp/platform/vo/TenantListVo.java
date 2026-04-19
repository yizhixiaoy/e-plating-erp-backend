package com.plating.erp.platform.vo;

import java.time.LocalDateTime;

/**
 * 租户列表响应 VO
 * 
 * 包含租户完整信息
 */
public record TenantListVo(
        Long id,
        String tenantName,
        String logoUrl,
        String shortCode,
        String contactName,
        String phone,
        LocalDateTime expireTime,
        Integer status,
        String domain,              // 自定义域名
        String welcomeText,         // 租户配置JSON
        Long createdBy,
        LocalDateTime createdAt,
        Long updatedBy,
        LocalDateTime updatedAt
) {
}
