package com.plating.erp.iam.controller;

import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.AuthzCacheService;
import com.plating.erp.common.security.CredentialRevocationService;
import com.plating.erp.common.security.RefreshTokenService;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.common.util.FileUploadUtils;
import com.plating.erp.common.util.StringUtil;
import com.plating.erp.iam.entity.DeptEntity;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.entity.UserRoleEntity;
import com.plating.erp.iam.mapper.DeptMapper;
import com.plating.erp.iam.mapper.UserMapper;
import com.plating.erp.iam.service.UserService;
import com.plating.erp.iam.vo.UserListVo;
import com.plating.erp.iam.vo.UserVo;
import com.plating.erp.message.service.EmailService;
import com.plating.erp.platform.service.TenantService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理控制器
 * 处理用户的增删改查、状态管理、密码重置、角色绑定等操作
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final AuthzCacheService authzCacheService;
    private final RefreshTokenService refreshTokenService;
    private final CredentialRevocationService credentialRevocationService;
    private final TenantService tenantService;
    private final EmailService emailService;

    public UserController(UserService userService,
                          UserMapper userMapper,
                          DeptMapper deptMapper,
                          AuthzCacheService authzCacheService,
                          RefreshTokenService refreshTokenService,
                          CredentialRevocationService credentialRevocationService,
                          TenantService tenantService,
                          EmailService emailService) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.deptMapper = deptMapper;
        this.authzCacheService = authzCacheService;
        this.refreshTokenService = refreshTokenService;
        this.credentialRevocationService = credentialRevocationService;
        this.tenantService = tenantService;
        this.emailService = emailService;
    }

    /**
     * 根据租户生成下一个可用账号
     * 规则：租户简称 + "-" + 4位序号，如 ZD-0001
     *
     * @param tenantId 租户ID
     * @return 生成的账号
     */
    @GetMapping("/generate-username")
    @PreAuthorize("@authz.hasPerm('user:add')")
    public ApiResponse<String> generateUsername(@RequestParam Long tenantId) {
        String username = userService.generateUsername(tenantId);
        return ApiResponse.ok(username);
    }

    /**
     * 远程搜索用户（按姓名/工号/手机号模糊匹配）
     * 用于部门/岗位的负责人选择器，仅返回在职用户
     * @param keyword  搜索关键词（姓名/工号/手机号）
     * @param tenantId 租户ID
     * @param limit    返回数量上限（默认20）
     */
    @GetMapping("/search")
    @PreAuthorize("@authz.hasPerm('dept:add') or @authz.hasPerm('dept:edit') or @authz.hasPerm('position:add') or @authz.hasPerm('position:edit') or @authz.hasPerm('user:add') or @authz.hasPerm('user:edit')")
    public ApiResponse<List<java.util.Map<String, Object>>> search(
            @RequestParam String keyword,
            @RequestParam Long tenantId,
            @RequestParam(defaultValue = "20") int limit) {
        var me = SecurityUtils.currentUser();
        Long queryTenantId = me.isSystem() ? tenantId : me.tenantId();
        if (queryTenantId == null || keyword == null || keyword.isBlank()) {
            return ApiResponse.ok(List.of());
        }
        List<UserEntity> users = userService.searchUsers(keyword, queryTenantId, Math.min(limit, 50));
        List<java.util.Map<String, Object>> result = users.stream().map(u -> {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("realName", u.getRealName());
            m.put("username", u.getUsername());
            return m;
        }).toList();
        return ApiResponse.ok(result);
    }

    /**
     * 创建用户
     *
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
        // 租户ID：前端指定则用前端值，否则根据当前用户判断
        if (body.tenantId() != null) {
            entity.setTenantId(body.tenantId());
        } else {
            entity.setTenantId(me.isSystem() ? 1L : me.tenantId());
        }
        // 账号：如果前端未提供，则自动生成
        entity.setUsername(body.username() == null || body.username().isBlank()
                ? userService.generateUsername(entity.getTenantId())
                : body.username());
        String realName = body.realName();
        entity.setRealName(realName);
        entity.setPasswordHash(body.password() == null ? generateDefaultPassword(realName) : body.password());
        entity.setAvatarUrl(FileUploadUtils.extractOssPath(body.avatarUrl() == null ? "" : body.avatarUrl()));
        entity.setDeptId(body.deptId());
        entity.setPosition(body.position() == null ? "" : body.position());
        entity.setLeaderUserId(body.leaderUserId());
        entity.setPhone(body.phone() == null ? "" : body.phone());
        // 邮箱由后端根据姓名和租户域名自动生成
        String generatedEmail = generateCompanyEmail(realName, entity.getTenantId());
        entity.setEmail(generatedEmail);
        entity.setUserType(1);
        entity.setStatus(0);
        entity.setCreatedBy(me.userId());
        entity.setUpdatedBy(me.userId());
        userService.save(entity);

        log.info("用户创建成功, userId={}, username={}", entity.getId(), entity.getUsername());

        // 创建后绑定角色
        if (body.roleIds() != null && !body.roleIds().isEmpty()) {
            userService.bindRoles(entity.getTenantId(), entity.getId(), body.roleIds());
            log.info("用户角色绑定成功, userId={}, roleIds={}", entity.getId(), body.roleIds());
        }
        
        // 如果用户有邮箱且使用默认密码,发送初始化密码邮件
        String plainPassword = null;
        if (body.password() == null) {
            plainPassword = generateDefaultPassword(realName);
        }
        
        if (entity.getEmail() != null && !entity.getEmail().isBlank() && plainPassword != null) {
            try {
                emailService.sendNewUserPasswordEmail(
                    entity.getId(),
                    entity.getEmail(),
                    entity.getRealName(),
                    entity.getUsername(),
                    plainPassword,
                    entity.getTenantId(),
                    me.userId()
                );
                log.info("新用户密码邮件已加入发送队列: userId={}, email={}", entity.getId(), entity.getEmail());
            } catch (Exception e) {
                log.error("发送新用户密码邮件异常: userId={}", entity.getId(), e);
            }
        }

        UserEntity result = userService.getById(entity.getId());
        if (result != null && result.getAvatarUrl() != null && !result.getAvatarUrl().isEmpty()) {
            result.setAvatarUrl(FileUploadUtils.getResourceUrl(result.getAvatarUrl(), "avatar.jpg"));
        }
        return ApiResponse.ok(result);
    }

    /**
     * 查询用户列表
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param deptId   部门ID筛选
     * @param status   状态筛选
     * @param keyword  关键字（支持姓名、账号、手机号、租户名称模糊查询）
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
        log.debug("查询用户列表, pageNum={}, pageSize={}, deptId={}, status={}, keyword={}, queryTenantId={}, username={} ",
                pageNum, pageSize, deptId, status, keyword, tenantId, me.username());

        var page = userService.page(pageNum, pageSize, deptId, status, keyword,
                me.isSystem() ? tenantId : me.tenantId());

        // 转换为 VO，填充部门名称和领导姓名
        // 批量查询创建人/更新人/领导姓名
        List<Long> userIds = page.records().stream()
                .flatMap(user -> java.util.stream.Stream.of(
                        user.getCreatedBy(), user.getUpdatedBy(), user.getLeaderUserId()))
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        List<UserEntity> relatedUsers = userIds.isEmpty() ? List.of() :
                userMapper.selectBatchIds(userIds);
        Map<Long, String> userNameMap = relatedUsers.stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getRealName, (a, b) -> a));
        Map<Long, String> userUsernameMap = relatedUsers.stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getUsername, (a, b) -> a));

        List<UserListVo> voList = page.records().stream().map(user -> {
            String deptName = null;
            if (user.getDeptId() != null) {
                DeptEntity dept = deptMapper.selectById(user.getDeptId());
                if (dept != null) {
                    deptName = dept.getDeptName();
                }
            }

            return new UserListVo(
                    user.getId(),
                    user.getTenantId(),
                    user.getTenantName(),
                    user.getShortName(),
                    user.getUsername(),
                    user.getRealName(),
                    FileUploadUtils.getResourceUrl(user.getAvatarUrl(), "avatar.jpg"),
                    user.getDeptId(),
                    deptName,
                    user.getPosition(),
                    user.getLeaderUserId(),
                    userNameMap.getOrDefault(user.getLeaderUserId(), null),
                    userUsernameMap.getOrDefault(user.getLeaderUserId(), null),
                    user.getPhone(),
                    user.getEmail(),
                    user.getUserType(),
                    user.getStatus(),
                    user.getLastLoginAt(),
                    user.getLastLoginIp(),
                    user.getLoginCount(),
                    user.getCreatedBy(),
                    userNameMap.getOrDefault(user.getCreatedBy(), null),
                    user.getCreatedAt(),
                    user.getUpdatedBy(),
                    userNameMap.getOrDefault(user.getUpdatedBy(), null),
                    user.getUpdatedAt()
            );
        }).toList();

        log.debug("用户列表查询完成, 总数={}", page.total());
        return ApiResponse.ok(new PageResult<>(voList, page.total()));
    }

    /**
     * 获取用户详情
     *
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
        // 头像URL转为完整资源URL
        if (u.getAvatarUrl() != null && !u.getAvatarUrl().isEmpty()) {
            u.setAvatarUrl(FileUploadUtils.getResourceUrl(u.getAvatarUrl(), "avatar.jpg"));
        }
        return ApiResponse.ok(u);
    }

    /**
     * 获取用户公开资料（聊天等场景，仅需登录，不需 user:view 权限）
     * 返回：realName, avatarUrl, position, deptName, companyName, phone, email
     */
    @GetMapping("/{userId}/profile")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> userProfile(@PathVariable Long userId) {
        UserEntity u = userService.getById(userId);
        if (u == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        // 构造公开资料
        Map<String, Object> profile = new java.util.LinkedHashMap<>();
        profile.put("id", u.getId());
        profile.put("realName", u.getRealName());
        profile.put("username", u.getUsername());
        profile.put("avatarUrl", u.getAvatarUrl() != null && !u.getAvatarUrl().isEmpty()
                ? FileUploadUtils.getResourceUrl(u.getAvatarUrl(), "avatar.jpg") : null);
        profile.put("position", u.getPosition());
        profile.put("phone", u.getPhone());
        profile.put("email", u.getEmail());
        // 部门
        String deptName = null;
        if (u.getDeptId() != null) {
            DeptEntity dept = deptMapper.selectById(u.getDeptId());
            if (dept != null) deptName = dept.getDeptName();
        }
        profile.put("deptName", deptName);
        // 公司
        String companyName = null;
        if (u.getTenantId() != null) {
            var tenant = tenantService.getById(u.getTenantId());
            if (tenant != null) companyName = tenant.getTenantName();
        }
        profile.put("companyName", companyName);
        return ApiResponse.ok(profile);
    }

    /**
     * 获取用户绑定的角色列表
     *
     * @param userId 用户ID
     * @return 用户角色列表
     */
    @GetMapping("/{userId}/roles")
    @PreAuthorize("@authz.hasPerm('user:view')")
    public ApiResponse<List<UserRoleEntity>> getUserRoles(@PathVariable Long userId) {
        var me = SecurityUtils.currentUser();
        UserEntity u = userService.getById(userId);
        if (u == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        assertUserTenant(u);
        List<UserRoleEntity> roles = userService.getUserRoles(u.getTenantId(), userId);
        return ApiResponse.ok(roles);
    }

    /**
     * 更新用户信息
     *
     * @param userId 用户ID
     * @param body   用户更新请求
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
        if (body.password() != null && !body.password().isBlank()) u.setPasswordHash(body.password());
        if (body.avatarUrl() != null) u.setAvatarUrl(StringUtil.blankToNull(FileUploadUtils.extractOssPath(body.avatarUrl())));
        if (body.deptId() != null) u.setDeptId(body.deptId());
        if (body.position() != null) u.setPosition(StringUtil.blankToNull(body.position()));
        if (body.leaderUserId() != null) u.setLeaderUserId(body.leaderUserId());
        if (body.phone() != null) u.setPhone(StringUtil.blankToNull(body.phone()));
        if (body.email() != null) u.setEmail(StringUtil.blankToNull(body.email()));
        u.setUpdatedBy(SecurityUtils.currentUser().userId());
        userService.save(u);

        // 更新角色绑定
        if (body.roleIds() != null) {
            userService.bindRoles(u.getTenantId(), userId, body.roleIds());
            log.info("用户角色更新成功, userId={}, roleIds={}", userId, body.roleIds());
        }

        log.info("用户更新成功, userId={}", userId);
        UserEntity result = userService.getById(u.getId());
        if (result != null && result.getAvatarUrl() != null && !result.getAvatarUrl().isEmpty()) {
            result.setAvatarUrl(FileUploadUtils.getResourceUrl(result.getAvatarUrl(), "avatar.jpg"));
        }
        return ApiResponse.ok(result);
    }

    /**
     * 修改用户状态（启用/停用）
     *
     * @param userId 用户ID
     * @param body   状态修改请求
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
        u.setUpdatedBy(SecurityUtils.currentUser().userId());
        userService.save(u);

        log.info("用户状态修改成功, userId={}, newStatus={}", userId, newStatus);
        UserEntity result = userService.getById(u.getId());
        if (result != null && result.getAvatarUrl() != null && !result.getAvatarUrl().isEmpty()) {
            result.setAvatarUrl(FileUploadUtils.getResourceUrl(result.getAvatarUrl(), "avatar.jpg"));
        }
        return ApiResponse.ok(result);
    }

    /**
     * 重置用户密码
     * 重置后会清除用户的权限缓存和所有刷新令牌
     *
     * @param userId 用户ID
     * @return 重置后的新密码
     */
    @PatchMapping("/{userId}/reset-password")
    @PreAuthorize("@authz.hasPerm('user:reset')")
    @AuditLog(module = "用户管理", operateType = "RESET_PASSWORD", bizModule = "user", fieldName = "passwordHash")
    public ApiResponse<CommonResponses.ResetPasswordResponse> resetPassword(@PathVariable Long userId) {
        log.info("重置用户密码, userId={}", userId);

        UserEntity u = userService.getById(userId);
        if (u == null) {
            log.warn("重置密码失败，用户不存在, userId={}", userId);
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        assertUserTenant(u);

        String plain = generateDefaultPassword(u.getRealName());
        u.setPasswordHash(plain);
        u.setUpdatedBy(SecurityUtils.currentUser().userId());
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
     *
     * @param userId 用户ID
     * @param body   角色绑定请求
     * @return 绑定结果
     */
    @PutMapping("/{userId}/roles")
    @PreAuthorize("@authz.hasPerm('role:grant')")
    @AuditLog(module = "用户管理", operateType = "BIND_ROLE", bizModule = "user_role", fieldName = "roleIds")
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
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 解绑结果
     */
    @DeleteMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("@authz.hasPerm('role:grant')")
    @AuditLog(module = "用户管理", operateType = "UNBIND_ROLE", bizModule = "user_role", fieldName = "roleId")
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
     *
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

    /**
     * 生成企业邮箱账号
     * 规则：姓名拼音全拼@企业域名，重名时追加数字后缀
     * 参考飞书/钉钉: zhangsan@zhiduyun.com, zhangsan01@zhiduyun.com
     */
    private String generateCompanyEmail(String realName, Long tenantId) {
        if (realName == null || realName.isBlank()) {
            return "";
        }
        // 获取租户域名作为邮箱后缀
        String domain = null;
        if (tenantId != null) {
            var tenant = tenantService.getById(tenantId);
            if (tenant != null && tenant.getDomain() != null && !tenant.getDomain().isBlank()) {
                domain = tenant.getDomain();
            }
        }
        if (domain == null || domain.isBlank()) {
            domain = "zhiduyun.com";
        }
        // 姓名转拼音全拼
        String pinyinPrefix = toPinyinFull(realName);
        if (pinyinPrefix.isEmpty()) {
            return "";
        }
        // 检查重复，追加数字后缀
        String candidate = pinyinPrefix + "@" + domain;
        int suffix = 1;
        while (userMapper.existsByEmail(candidate)) {
            candidate = pinyinPrefix + String.format("%02d", suffix) + "@" + domain;
            suffix++;
        }
        return candidate;
    }

    /**
     * 中文姓名转拼音全拼（小写无空格）
     * 张三 → zhangsan, John Smith → johnsmith
     */
    private String toPinyinFull(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN) {
                try {
                    String[] pinyin = PinyinHelper.toHanyuPinyinStringArray(ch, format);
                    if (pinyin != null && pinyin.length > 0 && !pinyin[0].isEmpty()) {
                        sb.append(pinyin[0]);
                    }
                } catch (Exception e) {
                    log.warn("拼音转换失败, char={}", ch, e);
                }
            } else if (Character.isLetter(ch)) {
                sb.append(Character.toLowerCase(ch));
            }
        }
        return sb.toString();
    }

    /**
     * 生成默认密码：Init@ + 姓名每个字首字母大写 + 3位随机数字
     * 示例：张三 → Init@ZS382，John → Init@J015
     */
    private String generateDefaultPassword(String realName) {
        String initials = getInitials(realName);
        int randomNum = (int) (Math.random() * 1000);
        return String.format("Init@%s%03d", initials, randomNum);
    }

    /**
     * 获取字符串每个字符的首字母（中文取拼音首字母，英文直接取大写）
     */
    private String getInitials(String name) {
        if (name == null || name.isEmpty()) {
            return "U";
        }
        StringBuilder sb = new StringBuilder();
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN) {
                try {
                    String[] pinyin = PinyinHelper.toHanyuPinyinStringArray(ch, format);
                    if (pinyin != null && pinyin.length > 0 && !pinyin[0].isEmpty()) {
                        sb.append(Character.toUpperCase(pinyin[0].charAt(0)));
                    }
                } catch (Exception e) {
                    log.warn("拼音转换失败, char={}", ch, e);
                }
            } else if (Character.isLetter(ch)) {
                sb.append(Character.toUpperCase(ch));
            }
        }
        return sb.length() > 0 ? sb.toString() : "U";
    }
}
