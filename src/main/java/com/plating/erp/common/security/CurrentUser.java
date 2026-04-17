package com.plating.erp.common.security;

import java.util.List;

/**
 * 当前登录用户信息
 * 
 * 用户类型说明：
 * - userType = 0：平台用户（系统管理员），关联平台租户(tenantId=1)，可访问所有租户数据
 * - userType = 1：租户用户（租户管理员、普通员工），关联具体租户，只能访问本租户数据
 */
public record CurrentUser(Long userId, Long tenantId, String username, List<String> roles, Integer userType) {
    
    /**
     * 判断是否为平台用户（系统管理员）
     * 平台用户可以管理所有租户的数据，跳过租户隔离
     * 
     * @return true 如果是平台用户
     */
    public boolean isSystem() {
        return userType != null && userType == 0;
    }
    
    /**
     * 判断是否为租户用户
     * 租户用户只能访问本租户的数据
     * 
     * @return true 如果是租户用户
     */
    public boolean isTenantUser() {
        return userType != null && userType == 1;
    }
}
