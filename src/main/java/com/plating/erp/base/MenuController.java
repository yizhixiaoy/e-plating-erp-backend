package com.plating.erp.base;

import com.plating.erp.base.vo.MenuVo;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.common.security.AuthzCacheService;
import com.plating.erp.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menus")
public class MenuController {
    private final AuthzCacheService authzCacheService;

    public MenuController(AuthzCacheService authzCacheService) {
        this.authzCacheService = authzCacheService;
    }

    @GetMapping("/tree")
    @PreAuthorize("@authz.hasPerm('menu:view')")
    public ApiResponse<List<CommonResponses.MenuItemResponse>> tree() {
        return ApiResponse.ok(List.of(
                new CommonResponses.MenuItemResponse(1L, "系统管理", "/"),
                new CommonResponses.MenuItemResponse(2L, "租户管理", "/tenants")
        ));
    }

    @GetMapping("/current-user")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<CommonResponses.MenuItemResponse>> currentUserMenus() {
        return tree();
    }

    @PostMapping
    @PreAuthorize("@authz.hasPerm('menu:add')")
    @AuditLog(module = "菜单管理", operateType = "CREATE", bizModule = "menu", fieldName = "menu_name")
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
    @AuditLog(module = "菜单管理", operateType = "UPDATE", bizModule = "menu", fieldName = "menu_name")
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
    @AuditLog(module = "菜单管理", operateType = "DELETE", bizModule = "menu", fieldName = "menu_name")
    public ApiResponse<CommonResponses.DeleteResponse> delete(@PathVariable Long menuId) {
        authzCacheService.evictTenant(SecurityUtils.currentUser().tenantId());
        return ApiResponse.ok(new CommonResponses.DeleteResponse(true, menuId));
    }
}
