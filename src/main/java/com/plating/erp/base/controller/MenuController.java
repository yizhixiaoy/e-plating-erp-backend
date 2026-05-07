package com.plating.erp.base.controller;

import com.plating.erp.base.vo.MenuVo;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.common.security.AuthzCacheService;
import com.plating.erp.common.security.SecurityUtils;
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

    @GetMapping("/current-user")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> currentUserMenus() {
        return ApiResponse.ok(menuService.getUserRoutesAndPermissions());
    }

    @PostMapping
    @PreAuthorize("@authz.hasPerm('menu:add')")
    @AuditLog(module = "菜单管理", operateType = "CREATE", bizModule = "menu", fieldName = "menuName")
    public ApiResponse<CommonResponses.MenuItemResponse> create(@Valid @RequestBody MenuVo.MenuCreateReq body) {
        ApiResponse<CommonResponses.MenuItemResponse> resp = ApiResponse.ok(new CommonResponses.MenuItemResponse(
                50001L,
                body.menuName() == null ? "新菜单" : body.menuName(),
                body.path() == null ? "/new" : body.path()
        ));
        authzCacheService.evictTenant(SecurityUtils.currentUser().tenantId());
        return resp;
    }

    @PutMapping("/{menuId}")
    @PreAuthorize("@authz.hasPerm('menu:edit')")
    @AuditLog(module = "菜单管理", operateType = "UPDATE", bizModule = "menu", fieldName = "menuName")
    public ApiResponse<CommonResponses.MenuItemResponse> update(@PathVariable Long menuId, @Valid @RequestBody MenuVo.MenuUpdateReq body) {
        ApiResponse<CommonResponses.MenuItemResponse> resp = ApiResponse.ok(new CommonResponses.MenuItemResponse(
                menuId,
                body.menuName() == null ? "菜单" : body.menuName(),
                body.path() == null ? "/updated" : body.path()
        ));
        authzCacheService.evictTenant(SecurityUtils.currentUser().tenantId());
        return resp;
    }

    @DeleteMapping("/{menuId}")
    @PreAuthorize("@authz.hasPerm('menu:delete')")
    @AuditLog(module = "菜单管理", operateType = "DELETE", bizModule = "menu", fieldName = "menuName")
    public ApiResponse<CommonResponses.DeleteResponse> delete(@PathVariable Long menuId) {
        authzCacheService.evictTenant(SecurityUtils.currentUser().tenantId());
        return ApiResponse.ok(new CommonResponses.DeleteResponse(true, menuId));
    }
}
