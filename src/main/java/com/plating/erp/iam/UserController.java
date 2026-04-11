package com.plating.erp.iam;

import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.service.UserService;
import com.plating.erp.iam.vo.UserVo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("@authz.hasPerm('user:add')")
    @AuditLog(module = "用户管理", operateType = "CREATE", bizModule = "user", fieldName = "username")
    public ApiResponse<UserEntity> create(@Valid @RequestBody UserVo.UserCreateReq body) {
        UserEntity entity = new UserEntity();
        entity.setTenantId(body.tenantId() == null ? 1L : body.tenantId());
        entity.setUsername(body.username() == null ? "a-00001" : body.username());
        entity.setPasswordHash(body.password() == null ? "123456" : body.password());
        entity.setRealName(body.realName() == null ? "新用户" : body.realName());
        entity.setAvatarUrl(body.avatarUrl() == null ? "" : body.avatarUrl());
        entity.setDeptId(body.deptId());
        entity.setPhone(body.phone() == null ? "" : body.phone());
        entity.setEmail(body.email() == null ? "" : body.email());
        entity.setUserType(1);
        entity.setStatus(0);
        return ApiResponse.ok(userService.save(entity));
    }

    @GetMapping
    @PreAuthorize("@authz.hasPerm('user:view')")
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "20") Integer pageSize,
                               @RequestParam(required = false) Long deptId,
                               @RequestParam(required = false) Integer status) {
        var page = userService.page(pageNum, pageSize, deptId, status);
        return ApiResponse.ok(new PageResult<>(page.getRecords(), page.getTotal()));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@authz.hasPerm('user:view')")
    public ApiResponse<UserEntity> detail(@PathVariable Long userId) {
        return ApiResponse.ok(userService.getById(userId));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("@authz.hasPerm('user:edit')")
    @AuditLog(module = "用户管理", operateType = "UPDATE", bizModule = "user", fieldName = "username")
    public ApiResponse<UserEntity> update(@PathVariable Long userId, @Valid @RequestBody UserVo.UserUpdateReq body) {
        UserEntity u = userService.getById(userId);
        if (u == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (body.username() != null) u.setUsername(body.username());
        if (body.realName() != null) u.setRealName(body.realName());
        if (body.avatarUrl() != null) u.setAvatarUrl(body.avatarUrl());
        if (body.deptId() != null) u.setDeptId(body.deptId());
        if (body.phone() != null) u.setPhone(body.phone());
        if (body.email() != null) u.setEmail(body.email());
        return ApiResponse.ok(userService.save(u));
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("@authz.hasPerm('user:status')")
    @AuditLog(module = "用户管理", operateType = "STATUS", bizModule = "user", fieldName = "status")
    public ApiResponse<UserEntity> updateStatus(@PathVariable Long userId, @Valid @RequestBody UserVo.UserStatusReq body) {
        UserEntity u = userService.getById(userId);
        if (u == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        u.setStatus(body.status() == null ? 0 : body.status());
        return ApiResponse.ok(userService.save(u));
    }

    @PatchMapping("/{userId}/reset-password")
    @PreAuthorize("@authz.hasPerm('user:reset')")
    @AuditLog(module = "用户管理", operateType = "RESET_PASSWORD", bizModule = "user", fieldName = "password_hash")
    public ApiResponse<CommonResponses.ResetPasswordResponse> resetPassword(@PathVariable Long userId) {
        return ApiResponse.ok(new CommonResponses.ResetPasswordResponse(userId, "Init@123456", true));
    }

    @PutMapping("/{userId}/roles")
    @PreAuthorize("@authz.hasPerm('role:grant')")
    @AuditLog(module = "用户管理", operateType = "BIND_ROLE", bizModule = "user_role", fieldName = "role_ids")
    public ApiResponse<CommonResponses.UserRoleBindResponse> bindRoles(@PathVariable Long userId,
                                                                       @Valid @RequestBody UserVo.UserRoleBindReq body) {
        Long tenantId = SecurityUtils.currentUser().tenantId();
        int bindCount = userService.bindRoles(tenantId, userId, body.roleIds());
        return ApiResponse.ok(new CommonResponses.UserRoleBindResponse(userId, bindCount, true));
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("@authz.hasPerm('role:grant')")
    @AuditLog(module = "用户管理", operateType = "UNBIND_ROLE", bizModule = "user_role", fieldName = "role_id")
    public ApiResponse<CommonResponses.DeleteResponse> unbindRole(@PathVariable Long userId, @PathVariable Long roleId) {
        Long tenantId = SecurityUtils.currentUser().tenantId();
        boolean deleted = userService.unbindRole(tenantId, userId, roleId);
        return ApiResponse.ok(new CommonResponses.DeleteResponse(deleted, roleId));
    }
}
