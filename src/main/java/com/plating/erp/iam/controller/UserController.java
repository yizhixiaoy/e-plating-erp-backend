package com.plating.erp.iam.controller;

import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.common.security.AuthzCacheService;
import com.plating.erp.common.security.CredentialRevocationService;
import com.plating.erp.common.security.RefreshTokenService;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.service.UserService;
import com.plating.erp.iam.vo.UserVo;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 * 处理用户的增删改查、状态管理、密码重置、角色绑定等操作
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final AuthzCacheService authzCacheService;
    private final RefreshTokenService refreshTokenService;
    private final CredentialRevocationService credentialRevocationService;

    public UserController(UserService userService,
                          AuthzCacheService authzCacheService,
                          RefreshTokenService refreshTokenService,
                          CredentialRevocationService credentialRevocationService) {
        this.userService = userService;
        this.authzCacheService = authzCacheService;
        this.refreshTokenService = refreshTokenService;
        this.credentialRevocationService = credentialRevocationService;
    }

    /**
     * 创建用户
     * @param body 用户创建请求
     * @return 创建成功的用户信息
     */
    @PostMapping
    @PreAuthorize("@authz.hasPerm('user:add')")
    @AuditLog(module = "用户管理", operateType = "CREATE", bizModule = "user", fieldName = "username")
    public ApiResponse<UserEntity> create(@Valid @RequestBody UserVo.UserCreateReq body) {
        var me = SecurityUtils.currentUser();
        log.info("创建用户请求, operator={}, username={}, tenantId={}", me.userId(), body.username(), me.tenantId());
        
        UserEntity entity = new UserEntity();
        long tid = body.tenantId() == null ? 1L : body.tenantId();
        entity.setTenantId(me.isSystem() ? tid : me.tenantId());
        entity.setUsername(body.username() == null ? "a-00001" : body.username());
        entity.setPasswordHash(body.password() == null ? "123456" : body.password());
        entity.setRealName(body.realName() == null ? "新用户" : body.realName());
        entity.setAvatarUrl(body.avatarUrl() == null ? "" : body.avatarUrl());
        entity.setDeptId(body.deptId());
        entity.setPhone(body.phone() == null ? "" : body.phone());
        entity.setEmail(body.email() == null ? "" : body.email());
        entity.setUserType(1);
        entity.setStatus(0);
        userService.save(entity);
        
        log.info("用户创建成功, userId={}, username={}", entity.getId(), entity.getUsername());
        return ApiResponse.ok(userService.getById(entity.getId()));
    }

    /**
     * 查询用户列表
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param deptId 部门ID筛选
     * @param status 状态筛选
     * @param keyword 关键字（姓名/账号/手机号）
     * @param tenantId 租户ID（平台管理员使用）
     * @return 用户分页列表
     */
    @GetMapping
    @PreAuthorize("@authz.hasPerm('user:view')")
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "20") Integer pageSize,
                               @RequestParam(required = false) Long deptId,
                               @RequestParam(required = false) Integer status,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Long tenantId) {
        var me = SecurityUtils.currentUser();
        log.debug("查询用户列表, pageNum={}, pageSize={}, deptId={}, status={}, keyword={}, queryTenantId={}, username={}",
                pageNum, pageSize, deptId, status, keyword, tenantId, me.username());
        
        var page = userService.page(pageNum, pageSize, deptId, status, keyword, tenantId, me.isSystem());
        log.debug("用户列表查询完成, 总数={}", page.total());
        return ApiResponse.ok(new PageResult<>(page.records(), page.total()));
    }

    /**
     * 获取用户详情
     * @param userId 用户ID
     * @return 用户详细信息
     */
    @GetMapping("/{userId}")
    @PreAuthorize("@authz.hasPerm('user:view')")
    public ApiResponse<UserEntity> detail(@PathVariable Long userId) {
        log.debug("获取用户详情, userId={}", userId);
        
        UserEntity u = userService.getById(userId);
        if (u == null) {
            log.warn("用户不存在, userId={}", userId);
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        assertUserTenant(u);
        return ApiResponse.ok(u);
    }

    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param body 用户更新请求
     * @return 更新后的用户信息
     */
    @PutMapping("/{userId}")
    @PreAuthorize("@authz.hasPerm('user:edit')")
    @AuditLog(module = "用户管理", operateType = "UPDATE", bizModule = "user", fieldName = "username")
    public ApiResponse<UserEntity> update(@PathVariable Long userId, @Valid @RequestBody UserVo.UserUpdateReq body) {
        log.info("更新用户请求, userId={}", userId);
        
        UserEntity u = userService.getById(userId);
        if (u == null) {
            log.warn("更新失败，用户不存在, userId={}", userId);
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        assertUserTenant(u);
        
        if (body.username() != null) u.setUsername(body.username());
        if (body.realName() != null) u.setRealName(body.realName());
        if (body.avatarUrl() != null) u.setAvatarUrl(body.avatarUrl());
        if (body.deptId() != null) u.setDeptId(body.deptId());
        if (body.phone() != null) u.setPhone(body.phone());
        if (body.email() != null) u.setEmail(body.email());
        userService.save(u);
        
        log.info("用户更新成功, userId={}", userId);
        return ApiResponse.ok(userService.getById(u.getId()));
    }

    /**
     * 修改用户状态（启用/停用）
     * @param userId 用户ID
     * @param body 状态修改请求
     * @return 修改后的用户信息
     */
    @PatchMapping("/{userId}/status")
    @PreAuthorize("@authz.hasPerm('user:status')")
    @AuditLog(module = "用户管理", operateType = "STATUS", bizModule = "user", fieldName = "status")
    public ApiResponse<UserEntity> updateStatus(@PathVariable Long userId, @Valid @RequestBody UserVo.UserStatusReq body) {
        log.info("修改用户状态, userId={}, status={}", userId, body.status());
        
        UserEntity u = userService.getById(userId);
        if (u == null) {
            log.warn("修改状态失败，用户不存在, userId={}", userId);
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        assertUserTenant(u);
        
        Integer newStatus = body.status() == null ? 0 : body.status();
        u.setStatus(newStatus);
        userService.save(u);
        
        log.info("用户状态修改成功, userId={}, newStatus={}", userId, newStatus);
        return ApiResponse.ok(userService.getById(u.getId()));
    }

    /**
     * 重置用户密码
     * 重置后会清除用户的权限缓存和所有刷新令牌
     * @param userId 用户ID
     * @return 重置后的新密码
     */
    @PatchMapping("/{userId}/reset-password")
    @PreAuthorize("@authz.hasPerm('user:reset')")
    @AuditLog(module = "用户管理", operateType = "RESET_PASSWORD", bizModule = "user", fieldName = "password_hash")
    public ApiResponse<CommonResponses.ResetPasswordResponse> resetPassword(@PathVariable Long userId) {
        log.info("重置用户密码, userId={}", userId);
        
        UserEntity u = userService.getById(userId);
        if (u == null) {
            log.warn("重置密码失败，用户不存在, userId={}", userId);
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        assertUserTenant(u);
        
        String plain = "Init@123456";
        u.setPasswordHash(plain);
        userService.save(u);
        
        Long tid = u.getTenantId();
        if (tid != null) {
            // 清除用户缓存和令牌，强制重新登录
            authzCacheService.evictUser(tid, userId);
            refreshTokenService.invalidateAllForUser(tid, userId);
            credentialRevocationService.revokeCredentialsIssuedBeforeNow(tid, userId);
            log.info("已清除用户缓存和令牌, userId={}", userId);
        }
        
        log.info("密码重置成功, userId={}", userId);
        return ApiResponse.ok(new CommonResponses.ResetPasswordResponse(userId, plain, true, tid != null));
    }

    /**
     * 为用户绑定角色
     * @param userId 用户ID
     * @param body 角色绑定请求
     * @return 绑定结果
     */
    @PutMapping("/{userId}/roles")
    @PreAuthorize("@authz.hasPerm('role:grant')")
    @AuditLog(module = "用户管理", operateType = "BIND_ROLE", bizModule = "user_role", fieldName = "role_ids")
    public ApiResponse<CommonResponses.UserRoleBindResponse> bindRoles(@PathVariable Long userId,
                                                                       @Valid @RequestBody UserVo.UserRoleBindReq body) {
        Long tenantId = SecurityUtils.currentUser().tenantId();
        log.info("绑定角色请求, userId={}, roleIds={}, tenantId={}", userId, body.roleIds(), tenantId);
        
        int bindCount = userService.bindRoles(tenantId, userId, body.roleIds());
        log.info("角色绑定成功, userId={}, bindCount={}", userId, bindCount);
        
        return ApiResponse.ok(new CommonResponses.UserRoleBindResponse(userId, bindCount, true));
    }

    /**
     * 解除用户角色绑定
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 解绑结果
     */
    @DeleteMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("@authz.hasPerm('role:grant')")
    @AuditLog(module = "用户管理", operateType = "UNBIND_ROLE", bizModule = "user_role", fieldName = "role_id")
    public ApiResponse<CommonResponses.DeleteResponse> unbindRole(@PathVariable Long userId, @PathVariable Long roleId) {
        Long tenantId = SecurityUtils.currentUser().tenantId();
        log.info("解除角色绑定, userId={}, roleId={}, tenantId={}", userId, roleId, tenantId);
        
        boolean deleted = userService.unbindRole(tenantId, userId, roleId);
        log.info("角色解绑{}，userId={}, roleId={}", deleted ? "成功" : "失败", userId, roleId);
        
        return ApiResponse.ok(new CommonResponses.DeleteResponse(deleted, roleId));
    }

    /**
     * 验证用户是否属于当前操作者的租户
     * 非系统管理员只能操作同租户的用户
     * @param u 用户实体
     */
    private void assertUserTenant(UserEntity u) {
        if (u == null) {
            return;
        }
        var me = SecurityUtils.currentUser();
        if (me.isSystem()) {
            return;
        }
        if (u.getTenantId() == null || !u.getTenantId().equals(me.tenantId())) {
            log.warn("无权访问用户, userId={}, userTenantId={}, operatorTenantId={}",
                    u.getId(), u.getTenantId(), me.tenantId());
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该用户");
        }
    }
}
