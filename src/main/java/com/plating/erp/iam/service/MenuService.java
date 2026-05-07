package com.plating.erp.iam.service;

import java.util.List;
import java.util.Map;

public interface MenuService {

    Map<String, Object> getUserRoutesAndPermissions();

    /**
     * 获取菜单树（用于角色菜单分配等管理场景）
     * 返回树形结构，包含 id、label、children、menuType 等字段
     */
    List<Map<String, Object>> getMenuTree();
}
