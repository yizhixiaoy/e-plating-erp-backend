package com.plating.erp.iam;

import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.common.security.AuthzCacheService;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.iam.entity.RoleEntity;
import com.plating.erp.iam.service.RoleService;
import com.plating.erp.iam.vo.RoleResponseVo;
import com.plating.erp.iam.vo.RoleVo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {
    private final RoleService roleService;
    private final AuthzCacheService authzCacheService;

    public RoleController(RoleService roleService, AuthzCacheService authzCacheService) {
        this.roleService = roleService;
        this.authzCacheService = authzCacheService;
    }

    @GetMapping
    @PreAuthorize("@authz.hasPerm('role:view')")
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "20") Integer pageSize,
                               @RequestParam(required = false) Integer status) {
        var page = roleService.page(pageNum, pageSize, status);
        return ApiResponse.ok(new PageResult<>(page.getRecords(), page.getTotal()));
    }

    @PostMapping
    @PreAuthorize("@authz.hasPerm('role:add')")
    @AuditLog(module = "角色管理", operateType = "CREATE", bizModule = "role", fieldName = "role_key")
    public ApiResponse<RoleEntity> create(@Valid @RequestBody RoleVo.RoleCreateReq body) {
        RoleEntity role = new RoleEntity();
        role.setTenantId(body.tenantId() == null ? 1L : body.tenantId());
        role.setRoleName(body.roleName() == null ? "新角色" : body.roleName());
        role.setRoleKey(body.roleKey() == null ? "NEW_ROLE" : body.roleKey());
        role.setDataScope(body.dataScope() == null ? 4 : body.dataScope());
        role.setStatus(0);
        RoleEntity saved = roleService.save(role);
        authzCacheService.evictTenant(saved.getTenantId());
        return ApiResponse.ok(saved);
    }

    @PutMapping("/{roleId}")
    @PreAuthorize("@authz.hasPerm('role:edit')")
    @AuditLog(module = "角色管理", operateType = "UPDATE", bizModule = "role", fieldName = "role_key")
    public ApiResponse<RoleEntity> update(@PathVariable Long roleId, @Valid @RequestBody RoleVo.RoleUpdateReq body) {
        RoleEntity role = new RoleEntity();
        role.setId(roleId);
        role.setTenantId(body.tenantId() == null ? 1L : body.tenantId());
        role.setRoleName(body.roleName() == null ? "新角色" : body.roleName());
        role.setRoleKey(body.roleKey() == null ? "NEW_ROLE" : body.roleKey());
        role.setDataScope(body.dataScope() == null ? 4 : body.dataScope());
        role.setStatus(body.status() == null ? 0 : body.status());
        RoleEntity saved = roleService.save(role);
        authzCacheService.evictTenant(saved.getTenantId());
        return ApiResponse.ok(saved);
    }

    @PutMapping("/{roleId}/menus")
    @PreAuthorize("@authz.hasPerm('role:grant')")
    @AuditLog(module = "角色管理", operateType = "GRANT_MENU", bizModule = "role", fieldName = "menu_ids")
    public ApiResponse<RoleResponseVo.RoleMenusResponse> assignMenus(@PathVariable Long roleId, @Valid @RequestBody RoleVo.RoleMenusReq body) {
        authzCacheService.evictTenant(SecurityUtils.currentUser().tenantId());
        return ApiResponse.ok(new RoleResponseVo.RoleMenusResponse(roleId, body.menuIds(), body.dataScope()));
    }

    @DeleteMapping("/{roleId}")
    @PreAuthorize("@authz.hasPerm('role:delete')")
    @AuditLog(module = "角色管理", operateType = "DELETE", bizModule = "role", fieldName = "role_key")
    public ApiResponse<CommonResponses.DeleteResponse> delete(@PathVariable Long roleId) {
        boolean deleted = roleService.delete(roleId);
        authzCacheService.evictTenant(SecurityUtils.currentUser().tenantId());
        return ApiResponse.ok(new CommonResponses.DeleteResponse(deleted, roleId));
    }
}
