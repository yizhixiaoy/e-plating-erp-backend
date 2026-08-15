package com.plating.erp.base.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单相关 VO
 */
public class MenuVo {

    /**
     * 新增菜单请求
     */
    public record MenuCreateReq(
            Long parentId,
            @NotBlank(message = "菜单名称不能为空")
            @Size(max = 50, message = "菜单名称长度不能超过50个字符")
            String menuName,
            @NotBlank(message = "菜单类型不能为空")
            String menuType,
            String path,
            String component,
            String icon,
            String perms,
            Integer sortNo,
            Integer visible,
            Integer status
    ) {
    }

    /**
     * 编辑菜单请求
     */
    public record MenuUpdateReq(
            Long parentId,
            @Size(max = 50, message = "菜单名称长度不能超过50个字符")
            String menuName,
            String menuType,
            String path,
            String component,
            String icon,
            String perms,
            Integer sortNo,
            Integer visible,
            Integer status
    ) {
    }

    /**
     * 菜单列表响应(管理页面用)
     */
    public record MenuListVo(
            Long id,
            Long parentId,
            String menuName,
            String menuType,
            String path,
            String component,
            String icon,
            String perms,
            Integer sortNo,
            Integer visible,
            Integer status,
            Long tenantId,
            Long createdBy,
            String createdByName,
            LocalDateTime createdAt,
            Long updatedBy,
            String updatedByName,
            LocalDateTime updatedAt
    ) {
    }

    /**
     * 菜单树节点(管理页面树形结构用)
     */
    public record MenuTreeNode(
            Long id,
            Long parentId,
            String menuName,
            String menuType,
            String path,
            String component,
            String icon,
            String perms,
            Integer sortNo,
            Integer visible,
            Integer status,
            Long tenantId,
            Long createdBy,
            String createdByName,
            LocalDateTime createdAt,
            Long updatedBy,
            String updatedByName,
            LocalDateTime updatedAt,
            List<MenuTreeNode> children
    ) {
    }
}