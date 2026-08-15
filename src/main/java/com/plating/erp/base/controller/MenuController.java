package com.plating.erp.base.controller;

import com.plating.erp.base.vo.MenuVo;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.common.security.AuthzCacheService;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.iam.entity.MenuEntity;
import com.plating.erp.iam.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/menus")
public class MenuController {
    private final AuthzCacheService authzCacheService;
    private final MenuService menuService;

    public MenuController(AuthzCacheService authzCacheService, MenuService menuService) {
        this.authzCacheService = authzCacheService;
        this.menuService = menuService;
    }

    /**
     * 获取菜单树（用于角色菜单分配等管理场景）
     * 返回树形结构，包含 id、label、children、menuType、perms 等字段
     */
    @GetMapping("/tree")
    @PreAuthorize("@authz.hasPerm('menu:view')")
    public ApiResponse<List<Map<String, Object>>> tree() {
        return ApiResponse.ok(menuService.getMenuTree());
    }

    /**
     * 获取当前用户的菜单和权限
     */
    @GetMapping("/current-user")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> currentUserMenus() {
        return ApiResponse.ok(menuService.getUserRoutesAndPermissions());
    }

    /**
     * 获取菜单管理树（用于菜单管理页面）
     * 返回完整字段的树形结构
     */
    @GetMapping
    @PreAuthorize("@authz.hasPerm('menu:view')")
    public ApiResponse<List<MenuVo.MenuTreeNode>> list() {
        return ApiResponse.ok(menuService.getMenuTreeForManagement());
    }

    /**
     * 新增菜单
     */
    @PostMapping
    @PreAuthorize("@authz.hasPerm('menu:add')")
    @AuditLog(module = "菜单管理", operateType = "CREATE", bizModule = "menu", fieldName = "menuName")
    public ApiResponse<MenuEntity> create(@Valid @RequestBody MenuVo.MenuCreateReq body) {
        var me = SecurityUtils.currentUser();

        MenuEntity entity = new MenuEntity();
        entity.setParentId(body.parentId() == null ? 0L : body.parentId());
        entity.setMenuName(body.menuName());
        entity.setMenuType(body.menuType());
        entity.setPath(body.path());
        entity.setComponent(body.component());
        entity.setIcon(body.icon());
        entity.setPerms(body.perms());
        entity.setSortNo(body.sortNo() == null ? 0 : body.sortNo());
        entity.setVisible(body.visible() == null ? 1 : body.visible());
        entity.setStatus(body.status() == null ? 0 : body.status());
        entity.setTenantId(me.isSystem() ? 0L : me.tenantId());
        entity.setCreatedBy(me.userId());
        entity.setUpdatedBy(me.userId());

        MenuEntity saved = menuService.addMenu(entity);
        authzCacheService.evictTenant(me.tenantId());
        return ApiResponse.ok(saved);
    }

    /**
     * 编辑菜单
     */
    @PutMapping("/{menuId}")
    @PreAuthorize("@authz.hasPerm('menu:edit')")
    @AuditLog(module = "菜单管理", operateType = "UPDATE", bizModule = "menu", fieldName = "menuName")
    public ApiResponse<MenuEntity> update(@PathVariable Long menuId, @Valid @RequestBody MenuVo.MenuUpdateReq body) {
        var me = SecurityUtils.currentUser();
        MenuEntity existing = menuService.getMenuById(menuId);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "菜单不存在");
        }
        assertMenuTenant(existing);

        MenuEntity entity = new MenuEntity();
        entity.setId(menuId);
        entity.setParentId(body.parentId() != null ? body.parentId() : existing.getParentId());
        entity.setMenuName(body.menuName() != null ? body.menuName() : existing.getMenuName());
        entity.setMenuType(body.menuType() != null ? body.menuType() : existing.getMenuType());
        entity.setPath(body.path() != null ? body.path() : existing.getPath());
        entity.setComponent(body.component() != null ? body.component() : existing.getComponent());
        entity.setIcon(body.icon() != null ? body.icon() : existing.getIcon());
        entity.setPerms(body.perms() != null ? body.perms() : existing.getPerms());
        entity.setSortNo(body.sortNo() != null ? body.sortNo() : existing.getSortNo());
        entity.setVisible(body.visible() != null ? body.visible() : existing.getVisible());
        entity.setStatus(body.status() != null ? body.status() : existing.getStatus());
        entity.setUpdatedBy(me.userId());

        menuService.updateMenu(entity);
        authzCacheService.evictTenant(me.tenantId());

        MenuEntity updated = menuService.getMenuById(menuId);
        return ApiResponse.ok(updated);
    }

    /**
     * 删除菜单（逻辑删除）
     */
    @DeleteMapping("/{menuId}")
    @PreAuthorize("@authz.hasPerm('menu:delete')")
    @AuditLog(module = "菜单管理", operateType = "DELETE", bizModule = "menu", fieldName = "menuName")
    public ApiResponse<CommonResponses.DeleteResponse> delete(@PathVariable Long menuId) {
        MenuEntity existing = menuService.getMenuById(menuId);
        if (existing == null) {
            return ApiResponse.ok(new CommonResponses.DeleteResponse(false, menuId));
        }
        assertMenuTenant(existing);

        // 检查是否有子菜单
        if (menuService.hasChildren(menuId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "该菜单下存在子菜单，无法删除");
        }

        boolean deleted = menuService.deleteMenu(menuId);
        authzCacheService.evictTenant(SecurityUtils.currentUser().tenantId());
        return ApiResponse.ok(new CommonResponses.DeleteResponse(deleted, menuId));
    }

    private void assertMenuTenant(MenuEntity menu) {
        var me = SecurityUtils.currentUser();
        if (me.isSystem()) {
            return;
        }
        if (menu.getTenantId() == null || !menu.getTenantId().equals(me.tenantId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该菜单");
        }
    }
}
