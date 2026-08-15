package com.plating.erp.iam.vo;

import java.time.LocalDateTime;

/**
 * 角色列表响应 VO
 *
 * 包含角色完整信息及关联的部门名称、租户公司名称等
 */
public record RoleListVo(
        Long id,
        Long tenantId,
        String companyName,        // 租户公司名称
        Long deptId,
        String deptName,           // 部门名称
        String roleName,
        String roleKey,
        Integer dataScope,
        Integer status,
        Long createdBy,
        String createdByName,      // 创建人姓名
        LocalDateTime createdAt,
        Long updatedBy,
        String updatedByName,      // 更新人姓名
        LocalDateTime updatedAt
) {
}
