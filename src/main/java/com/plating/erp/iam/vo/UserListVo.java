package com.plating.erp.iam.vo;

import java.time.LocalDateTime;

/**
 * 用户列表响应 VO
 * 
 * 包含用户完整信息及关联的部门名称、领导姓名等
 */
public record UserListVo(
        Long id,
        Long tenantId,
        String companyName,
        String shortName,
        String username,
        String realName,
        String avatarUrl,
        Long deptId,
        String deptName,           // 部门名称
        String position,           // 岗位
        Long leaderUserId,
        String leaderName,         // 直属领导姓名
        String phone,
        String email,
        Integer userType,
        Integer status,
        LocalDateTime lastLoginAt,
        String lastLoginIp,
        Integer loginCount,
        Long createdBy,
        LocalDateTime createdAt,
        Long updatedBy,
        LocalDateTime updatedAt
) {
}
