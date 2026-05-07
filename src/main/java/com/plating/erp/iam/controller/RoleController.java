package com.plating.erp.iam.controller;

import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.common.security.AuthzCacheService;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.iam.entity.DeptEntity;
import com.plating.erp.iam.entity.RoleEntity;
import com.plating.erp.iam.entity.RoleMenuEntity;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.mapper.DeptMapper;
import com.plating.erp.iam.mapper.RoleMenuMapper;
import com.plating.erp.iam.mapper.UserMapper;
import com.plating.erp.iam.service.RoleService;
import com.plating.erp.iam.vo.RoleListVo;
import com.plating.erp.iam.vo.RoleResponseVo;
import com.plating.erp.iam.vo.RoleVo;
import com.plating.erp.platform.entity.TenantEntity;
import com.plating.erp.platform.mapper.TenantMapper;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {
    private final RoleService roleService;
    private final AuthzCacheService authzCacheService;
    private final RoleMenuMapper roleMenuMapper;
    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final TenantMapper tenantMapper;

    public RoleController(RoleService roleService, AuthzCacheService authzCacheService, RoleMenuMapper roleMenuMapper,
                          UserMapper userMapper, DeptMapper deptMapper, TenantMapper tenantMapper) {
        this.roleService = roleService;
        this.authzCacheService = authzCacheService;
        this.roleMenuMapper = roleMenuMapper;
        this.userMapper = userMapper;
        this.deptMapper = deptMapper;
        this.tenantMapper = tenantMapper;
    }

    @GetMapping
    @PreAuthorize("@authz.hasPerm('role:view')")
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "20") Integer pageSize,
                               @RequestParam(required = false) Integer status,
                               @RequestParam(required = false) Long filterTenantId,
                               @RequestParam(required = false) Long filterDeptId,
                               @RequestParam(required = false) String keyword) {
        var me = SecurityUtils.currentUser();
        var page = roleService.page(pageNum, pageSize, status, me.tenantId(), me.isSystem(),
                filterTenantId, filterDeptId, keyword);

        // 批量查询创建人/更新人姓名
        List<Long> userIds = page.records().stream()
                .flatMap(role -> java.util.stream.Stream.of(role.getCreatedBy(), role.getUpdatedBy()))
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        Map<Long, String> userNameMap = userIds.isEmpty() ? Map.of() :
                userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(UserEntity::getId, UserEntity::getRealName, (a, b) -> a));

        // 批量查询租户名称
        List<Long> tenantIds = page.records().stream()
                .map(RoleEntity::getTenantId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        Map<Long, String> tenantNameMap = tenantIds.isEmpty() ? Map.of() :
                tenantMapper.selectBatchIds(tenantIds).stream()
                        .collect(Collectors.toMap(TenantEntity::getId, TenantEntity::getTenantName, (a, b) -> a));

        // 批量查询部门名称
        List<Long> deptIds = page.records().stream()
                .map(RoleEntity::getDeptId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        Map<Long, String> deptNameMap = deptIds.isEmpty() ? Map.of() :
                deptMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(DeptEntity::getId, DeptEntity::getDeptName, (a, b) -> a));

        List<RoleListVo> voList = page.records().stream().map(role -> new RoleListVo(
                role.getId(),
                role.getTenantId(),
                tenantNameMap.getOrDefault(role.getTenantId(), null),
                role.getDeptId(),
                deptNameMap.getOrDefault(role.getDeptId(), null),
                role.getRoleName(),
                role.getRoleKey(),
                role.getDataScope(),
                role.getStatus(),
                role.getCreatedBy(),
                userNameMap.getOrDefault(role.getCreatedBy(), null),
                role.getCreatedAt(),
                role.getUpdatedBy(),
                userNameMap.getOrDefault(role.getUpdatedBy(), null),
                role.getUpdatedAt()
        )).toList();

        return ApiResponse.ok(new PageResult<>(voList, page.total()));
    }

    /**
     * 获取角色下拉选项（支持按公司和部门过滤）
     */
    @GetMapping("/options")
    @PreAuthorize("@authz.hasPerm('user:add') or @authz.hasPerm('user:edit') or @authz.hasPerm('role:view')")
    public ApiResponse<List<RoleEntity>> options(@RequestParam(required = false) Long tenantId,
                                                  @RequestParam(required = false) Long deptId) {
        var me = SecurityUtils.currentUser();
        Long queryTenantId = me.isSystem() ? tenantId : me.tenantId();
        if (queryTenantId == null) {
            return ApiResponse.ok(List.of());
        }
        List<RoleEntity> roles = roleService.listByTenantAndDept(queryTenantId, me.isSystem(), deptId);
        return ApiResponse.ok(roles);
    }

    @PostMapping
    @PreAuthorize("@authz.hasPerm('role:add')")
    @AuditLog(module = "角色管理", operateType = "CREATE", bizModule = "role", fieldName = "roleKey")
    public ApiResponse<RoleEntity> create(@Valid @RequestBody RoleVo.RoleCreateReq body) {
        var me = SecurityUtils.currentUser();
        RoleEntity role = new RoleEntity();
        long tid = body.tenantId() == null ? 1L : body.tenantId();
        role.setTenantId(me.isSystem() ? tid : me.tenantId());
        role.setRoleName(body.roleName() == null ? "新角色" : body.roleName());
        role.setRoleKey(body.roleKey() == null ? "NEW_ROLE" : body.roleKey());
        role.setDataScope(body.dataScope() == null ? 4 : body.dataScope());
        role.setDeptId(body.deptId());
        role.setStatus(0);
        role.setCreatedBy(me.userId());
        role.setUpdatedBy(me.userId());
        roleService.save(role);
        RoleEntity saved = roleService.getById(role.getId());
        authzCacheService.evictTenant(saved.getTenantId());
        return ApiResponse.ok(saved);
    }

    @PutMapping("/{roleId}")
    @PreAuthorize("@authz.hasPerm('role:edit')")
    @AuditLog(module = "角色管理", operateType = "UPDATE", bizModule = "role", fieldName = "roleKey")
    public ApiResponse<RoleEntity> update(@PathVariable Long roleId, @Valid @RequestBody RoleVo.RoleUpdateReq body) {
        RoleEntity existing = roleService.getById(roleId);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        assertRoleTenant(existing);
        RoleEntity role = new RoleEntity();
        role.setId(roleId);
        var me = SecurityUtils.currentUser();
        role.setTenantId(me.isSystem() && body.tenantId() != null ? body.tenantId() : existing.getTenantId());
        role.setRoleName(body.roleName() == null ? "新角色" : body.roleName());
        role.setRoleKey(body.roleKey() == null ? "NEW_ROLE" : body.roleKey());
        role.setDataScope(body.dataScope() == null ? 4 : body.dataScope());
        role.setDeptId(body.deptId() != null ? body.deptId() : existing.getDeptId());
        role.setStatus(body.status() == null ? 0 : body.status());
        role.setUpdatedBy(me.userId());
        roleService.save(role);
        RoleEntity saved = roleService.getById(role.getId());
        authzCacheService.evictTenant(saved.getTenantId());
        return ApiResponse.ok(saved);
    }

    /**
     * 获取角色已分配的菜单ID列表
     */
    @GetMapping("/{roleId}/menus")
    @PreAuthorize("@authz.hasPerm('role:view')")
    public ApiResponse<List<Long>> getRoleMenus(@PathVariable Long roleId) {
        RoleEntity existing = roleService.getById(roleId);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        assertRoleTenant(existing);
        List<Long> menuIds = roleMenuMapper.selectMenuIdsByRoleId(existing.getTenantId(), roleId);
        return ApiResponse.ok(menuIds);
    }

    @PutMapping("/{roleId}/menus")
    @PreAuthorize("@authz.hasPerm('role:grant')")
    @AuditLog(module = "角色管理", operateType = "GRANT_MENU", bizModule = "role", fieldName = "menuIds")
    @Transactional
    public ApiResponse<RoleResponseVo.RoleMenusResponse> assignMenus(@PathVariable Long roleId, @Valid @RequestBody RoleVo.RoleMenusReq body) {
        RoleEntity existing = roleService.getById(roleId);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        assertRoleTenant(existing);
        
        Long tenantId = existing.getTenantId();
        Long userId = SecurityUtils.currentUser().userId();
        
        // 删除原有角色菜单关联
        roleMenuMapper.deleteByRoleId(tenantId, roleId);
        
        // 保存新的角色菜单关联
        List<Long> menuIds = body.menuIds();
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds) {
                RoleMenuEntity roleMenu = new RoleMenuEntity();
                roleMenu.setTenantId(tenantId);
                roleMenu.setRoleId(roleId);
                roleMenu.setMenuId(menuId);
                roleMenu.setCreatedBy(userId);
                roleMenu.setCreatedAt(LocalDateTime.now());
                roleMenuMapper.insert(roleMenu);
            }
        }
        
        // 更新角色的数据范围
        if (body.dataScope() != null) {
            existing.setDataScope(body.dataScope());
            roleService.updateById(existing);
        }
        
        authzCacheService.evictTenant(tenantId);
        return ApiResponse.ok(new RoleResponseVo.RoleMenusResponse(roleId, menuIds, body.dataScope()));
    }

    @DeleteMapping("/{roleId}")
    @PreAuthorize("@authz.hasPerm('role:delete')")
    @AuditLog(module = "角色管理", operateType = "DELETE", bizModule = "role", fieldName = "roleKey")
    public ApiResponse<CommonResponses.DeleteResponse> delete(@PathVariable Long roleId) {
        var me = SecurityUtils.currentUser();
        RoleEntity existing = roleService.getById(roleId);
        if (existing != null) {
            assertRoleTenant(existing);
        }
        boolean deleted = roleService.delete(roleId, me.tenantId(), me.isSystem());
        if (deleted && existing != null) {
            authzCacheService.evictTenant(existing.getTenantId());
        }
        return ApiResponse.ok(new CommonResponses.DeleteResponse(deleted, roleId));
    }

    private void assertRoleTenant(RoleEntity role) {
        var me = SecurityUtils.currentUser();
        if (me.isSystem()) {
            return;
        }
        if (role.getTenantId() == null || !role.getTenantId().equals(me.tenantId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该角色");
        }
    }
}
