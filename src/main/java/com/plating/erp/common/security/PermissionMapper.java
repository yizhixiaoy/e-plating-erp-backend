package com.plating.erp.common.security;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper {
    @Select("""
            SELECT DISTINCT r.role_key
            FROM sys_user_role ur
            JOIN sys_role r ON ur.role_id = r.id AND ur.tenant_id = r.tenant_id
            WHERE ur.user_id = #{userId} AND ur.tenant_id = #{tenantId}
            """)
    List<String> selectRoleKeys(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    @Select("""
            SELECT DISTINCT m.perms
            FROM sys_user_role ur
            JOIN sys_role_menu rm ON ur.role_id = rm.role_id AND ur.tenant_id = rm.tenant_id
            JOIN sys_menu m ON rm.menu_id = m.id
            WHERE ur.user_id = #{userId} AND ur.tenant_id = #{tenantId}
              AND m.perms IS NOT NULL AND m.perms <> ''
            """)
    List<String> selectPerms(@Param("userId") Long userId, @Param("tenantId") Long tenantId);
}
