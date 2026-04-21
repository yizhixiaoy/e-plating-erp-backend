package com.plating.erp.iam.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.plating.erp.iam.entity.MenuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单Mapper
 */
@Mapper
public interface MenuMapper extends BaseMapper<MenuEntity> {

    /**
     * 根据用户ID查询菜单树(包含所有层级)
     * 关联 sys_user_role -> sys_role_menu -> sys_menu
     * 
     * @param userId 用户ID
     * @return 用户有权访问的菜单列表
     */
    @Select("""
        SELECT DISTINCT m.*
        FROM sys_menu m
        INNER JOIN sys_role_menu rm ON m.id = rm.menu_id
        INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id
        WHERE ur.user_id = #{userId}
          AND m.status = 0
          AND m.deleted = 0
        ORDER BY m.sort_no ASC
    """)
    List<MenuEntity> selectMenuTreeByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询权限标识列表
     * 
     * @param userId 用户ID
     * @return 权限标识列表，如: ["tenant:view", "user:add"]
     */
    @Select("""
        SELECT DISTINCT m.perms
        FROM sys_menu m
        INNER JOIN sys_role_menu rm ON m.id = rm.menu_id
        INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id
        WHERE ur.user_id = #{userId}
          AND m.menu_type = 'F'
          AND m.perms IS NOT NULL
          AND m.perms != ''
          AND m.status = 0
          AND m.deleted = 0
    """)
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    /**
     * 查询平台级菜单(tenant_id=0)
     * 用于租户管理员获取平台级公共菜单
     * 
     * @return 平台级菜单列表
     */
    @Select("""
        SELECT * FROM sys_menu
        WHERE tenant_id = 0
          AND status = 0
          AND deleted = 0
        ORDER BY sort_no ASC
    """)
    List<MenuEntity> selectPlatformMenus();

    /**
     * 查询所有菜单(包括平台级和租户级)
     * 用于系统管理员(tenant_id=1)获取完整菜单
     * 
     * @return 所有菜单列表
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
        SELECT * FROM sys_menu
        WHERE status = 0
          AND deleted = 0
        ORDER BY tenant_id ASC, sort_no ASC
    """)
    List<MenuEntity> selectAllMenus();

    /**
     * 查询租户级菜单
     * 
     * @param tenantId 租户ID
     * @return 租户级菜单列表
     */
    @Select("""
        SELECT * FROM sys_menu
        WHERE tenant_id = #{tenantId}
          AND status = 0
          AND deleted = 0
        ORDER BY sort_no ASC
    """)
    List<MenuEntity> selectTenantMenus(@Param("tenantId") Long tenantId);
}
