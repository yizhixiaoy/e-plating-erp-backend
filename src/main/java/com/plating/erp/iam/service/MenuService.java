package com.plating.erp.iam.service;

import com.plating.erp.base.vo.MenuVo;
import com.plating.erp.iam.entity.MenuEntity;

import java.util.List;
import java.util.Map;

public interface MenuService {

    Map<String, Object> getUserRoutesAndPermissions();

    /**
     * 获取菜单树（用于角色菜单分配等管理场景）
     * 返回树形结构，包含 id、label、children、menuType 等字段
     */
    List<Map<String, Object>> getMenuTree();

    /**
     * 获取菜单管理树（用于菜单管理页面）
     * 返回完整字段的树形结构
     */
    List<MenuVo.MenuTreeNode> getMenuTreeForManagement();

    /**
     * 新增菜单
     */
    MenuEntity addMenu(MenuEntity menu);

    /**
     * 更新菜单
     */
    boolean updateMenu(MenuEntity menu);

    /**
     * 删除菜单（逻辑删除）
     */
    boolean deleteMenu(Long menuId);

    /**
     * 根据ID获取菜单
     */
    MenuEntity getMenuById(Long menuId);

    /**
     * 检查是否有子菜单
     */
    boolean hasChildren(Long menuId);
}
