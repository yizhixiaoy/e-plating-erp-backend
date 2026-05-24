package com.plating.erp.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.plating.erp.auth.entity.LoginHistoryEntity;
import com.plating.erp.auth.entity.UserRecentTenantEntity;
import com.plating.erp.auth.mapper.LoginHistoryMapper;
import com.plating.erp.auth.mapper.UserRecentTenantMapper;
import com.plating.erp.auth.service.AuthService;
import com.plating.erp.auth.service.LoginSecurityService;
import com.plating.erp.auth.service.ScanLoginService;
import com.plating.erp.auth.service.VerificationCodeService;
import com.plating.erp.auth.vo.AuthResponseVo;
import com.plating.erp.auth.vo.AuthVo;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.security.JwtTokenService;
import com.plating.erp.common.security.PermissionMapper;
import com.plating.erp.common.security.RefreshTokenService;
import com.plating.erp.common.security.SessionKeyService;
import com.plating.erp.common.util.FileUploadUtils;
import com.plating.erp.iam.entity.DeptEntity;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.mapper.DeptMapper;
import com.plating.erp.iam.mapper.UserMapper;
import com.plating.erp.iam.service.UserService;
import com.plating.erp.platform.entity.TenantEntity;
import com.plating.erp.platform.mapper.TenantMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final TenantMapper tenantMapper;
    private final UserMapper userMapper;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final LoginSecurityService loginSecurityService;
    private final ScanLoginService scanLoginService;
    private final VerificationCodeService verificationCodeService;
    private final UserService userService;
    private final PermissionMapper permissionMapper;
    private final UserRecentTenantMapper userRecentTenantMapper;
    private final DeptMapper deptMapper;
    private final LoginHistoryMapper loginHistoryMapper;
    private final SessionKeyService sessionKeyService;

    public AuthServiceImpl(TenantMapper tenantMapper, UserMapper userMapper,
                           JwtTokenService jwtTokenService,
                           RefreshTokenService refreshTokenService,
                           LoginSecurityService loginSecurityService, ScanLoginService scanLoginService,
                           VerificationCodeService verificationCodeService, UserService userService,
                           PermissionMapper permissionMapper, UserRecentTenantMapper userRecentTenantMapper,
                           DeptMapper deptMapper, LoginHistoryMapper loginHistoryMapper,
                           SessionKeyService sessionKeyService) {
        this.tenantMapper = tenantMapper;
        this.userMapper = userMapper;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.loginSecurityService = loginSecurityService;
        this.scanLoginService = scanLoginService;
        this.verificationCodeService = verificationCodeService;
        this.userService = userService;
        this.permissionMapper = permissionMapper;
        this.userRecentTenantMapper = userRecentTenantMapper;
        this.deptMapper = deptMapper;
        this.loginHistoryMapper = loginHistoryMapper;
        this.sessionKeyService = sessionKeyService;
    }

    private List<String> resolveUserRoles(Long userId, Long tenantId, Integer userType) {
        List<String> roles = permissionMapper.selectRoleKeys(userId, tenantId);
        if (roles == null || roles.isEmpty()) {
            // 根据用户类型分配默认角色，而非硬编码用户名
            roles = (userType != null && userType == 0) ? List.of("PLATFORM_ADMIN") : List.of("TENANT_ADMIN");
        }
        return roles;
    }

    @Override
    public List<AuthResponseVo.TenantSearchResult> searchTenants(String keyword, Integer limit) {
        int actualLimit = limit != null ? limit : 10;
        LambdaQueryWrapper<TenantEntity> wrapper = new LambdaQueryWrapper<TenantEntity>()
                .eq(TenantEntity::getStatus, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(q -> q
                    .like(TenantEntity::getShortCode, keyword)
                    .or()
                    .like(TenantEntity::getTenantName, keyword)
            );
        }
        wrapper.last("LIMIT " + actualLimit);

        List<TenantEntity> tenants = tenantMapper.selectList(wrapper);

        List<AuthResponseVo.TenantSearchResult> result = new ArrayList<>();
        for (TenantEntity tenant : tenants) {
            result.add(new AuthResponseVo.TenantSearchResult(
                    tenant.getShortCode(),
                    tenant.getTenantName(),
                    FileUploadUtils.getResourceUrl(tenant.getLogoUrl(), "logo.png")
            ));
        }
        return result;
    }

    /**
     * 根据用户名查询用户所属租户信息
     * 用于登录页面自动反显企业信息
     * 
     * 说明：
     * - 平台用户（user_type=0）：不返回租户信息，前端不显示企业信息
     * - 租户用户（user_type=1）：返回所属租户信息
     * 
     * @param username 用户名
     * @return 租户信息，如果是平台用户、用户不存在或租户被禁用则返回found=false
     */
    @Override
    public AuthResponseVo.TenantByUsernameResult getTenantByUsername(String username) {
        log.debug("开始查询用户所属租户, username={}", username);
        
        // 根据用户名查询用户及其租户信息（使用认证专用查询方法，跳过租户拦截器）
        UserEntity user = userMapper.selectByUsernameForAuth(username);
        
        if (user == null) {
            log.warn("未找到用户, username={}", username);
            return new AuthResponseVo.TenantByUsernameResult(null, null, null, null, null, null, null, false);
        }
        
        // 平台用户（系统管理员）不需要返回租户信息
        boolean isPlatformUser = user.getUserType() != null && user.getUserType() == 0;
        if (isPlatformUser) {
            log.debug("平台用户，不返回租户信息, username={}, userType={}", username, user.getUserType());
            return new AuthResponseVo.TenantByUsernameResult(null, null, null, null, null, null, null, false);
        }
        
        log.debug("找到用户, userId={}, tenantId={}", user.getId(), user.getTenantId());
        
        // sys_tenant 表在白名单中，不受租户拦截器影响
        TenantEntity tenant = tenantMapper.selectById(user.getTenantId());
        if (tenant == null) {
            log.warn("用户所属租户不存在, userId={}, tenantId={}", user.getId(), user.getTenantId());
            return new AuthResponseVo.TenantByUsernameResult(null, null, null, null, null, null, null, false);
        }
        
        if (tenant.getStatus() != 0) {
            log.warn("用户所属租户已被禁用, tenantId={}, status={}", tenant.getId(), tenant.getStatus());
            return new AuthResponseVo.TenantByUsernameResult(null, null, null, null, null, null, null, false);
        }
        
        log.info("成功获取用户所属租户, username={}, tenantName={}", username, tenant.getTenantName());
        return new AuthResponseVo.TenantByUsernameResult(
                tenant.getShortCode(),
                tenant.getTenantName(),
                FileUploadUtils.getResourceUrl(tenant.getLogoUrl(), "logo.png"),
                tenant.getId(),
                user.getRealName(),
                user.getPhone(),
                user.getEmail(),
                true
        );
    }

    @Override
    public AuthResponseVo.TenantByPhoneResult getTenantByPhone(String phone) {
        log.debug("开始查询手机号所属租户, phone={}", phone);
        
        // 根据手机号查询用户（使用认证专用查询方法，跳过租户拦截器）
        UserEntity user = userMapper.selectByPhoneForAuth(phone, null);
        
        if (user == null) {
            log.warn("未找到该手机号的用户, phone={}", phone);
            throw new BizException(ErrorCode.BAD_REQUEST, "该手机号未注册");
        }
        
        // 校验用户状态
        if (user.getStatus() != null && user.getStatus() != 0) {
            log.warn("用户已被禁用, userId={}, status={}", user.getId(), user.getStatus());
            throw new BizException(ErrorCode.BAD_REQUEST, "您的账户已被禁用，请联系管理员");
        }
        
        // 平台用户返回system租户编码
        boolean isPlatformUser = user.getUserType() != null && user.getUserType() == 0;
        
        // 检查用户是否被锁定（通过检查登录尝试次数）
        try {
            String tenantCodeForCheck;
            if (isPlatformUser) {
                tenantCodeForCheck = "system";
            } else {
                TenantEntity tenant = tenantMapper.selectById(user.getTenantId());
                tenantCodeForCheck = tenant != null ? tenant.getShortCode() : "default";
            }
            loginSecurityService.checkLoginAttempt(user.getUsername(), tenantCodeForCheck);
        } catch (BizException e) {
            log.warn("用户账户被锁定, userId={}, username={}", user.getId(), user.getUsername());
            throw e;
        }
        
        if (isPlatformUser) {
            log.debug("平台用户，返回system租户, phone={}", phone);
            return new AuthResponseVo.TenantByPhoneResult("system", "平台系统", 1L, true);
        }
        
        log.debug("找到用户, userId={}, tenantId={}", user.getId(), user.getTenantId());
        
        // 查询租户信息
        TenantEntity tenant = tenantMapper.selectById(user.getTenantId());
        if (tenant == null) {
            log.warn("用户所属租户不存在, userId={}, tenantId={}", user.getId(), user.getTenantId());
            throw new BizException(ErrorCode.BAD_REQUEST, "您所属的租户不存在");
        }
        
        if (tenant.getStatus() != 0) {
            log.warn("用户所属租户已被禁用, tenantId={}, status={}", tenant.getId(), tenant.getStatus());
            throw new BizException(ErrorCode.BAD_REQUEST, "您所属的租户已被禁用，请联系管理员");
        }
        
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(java.time.LocalDateTime.now())) {
            log.warn("用户所属租户已过期, tenantId={}, expireTime={}", tenant.getId(), tenant.getExpireTime());
            throw new BizException(ErrorCode.BAD_REQUEST, "您所属的租户已过期，请联系管理员续费");
        }
        
        log.info("成功获取手机号所属租户, phone={}, tenantName={}", phone, tenant.getTenantName());
        return new AuthResponseVo.TenantByPhoneResult(
                tenant.getShortCode(),
                tenant.getTenantName(),
                tenant.getId(),
                true
        );
    }

    @Override
    public List<AuthResponseVo.RecentTenantResult> getRecentTenants(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return new ArrayList<>();
        }
        Long userId = jwtTokenService.getUserIdFromToken(auth.substring(7));
        if (userId == null) {
            return new ArrayList<>();
        }
        
        // 查询最近登录的租户
        List<UserRecentTenantEntity> recentTenants = userRecentTenantMapper.selectList(
                new LambdaQueryWrapper<UserRecentTenantEntity>()
                        .eq(UserRecentTenantEntity::getUserId, userId)
                        .orderByDesc(UserRecentTenantEntity::getLastLoginTime)
                        .last("LIMIT 10")
        );
        
        List<AuthResponseVo.RecentTenantResult> result = new ArrayList<>();
        for (UserRecentTenantEntity recent : recentTenants) {
            TenantEntity tenant = tenantMapper.selectById(recent.getTenantId());
            if (tenant != null) {
                result.add(new AuthResponseVo.RecentTenantResult(
                        tenant.getShortCode(),
                        tenant.getTenantName(),
                        FileUploadUtils.getResourceUrl(tenant.getLogoUrl(), "logo.png"),
                        recent.getLastLoginTime()
                ));
            }
        }
        return result;
    }

    /**
     * 用户登录
     * 支持多种登录方式：密码、短信验证码、邮箱验证码、扫码登录
     * 
     * 用户类型说明：
     * - user_type = 0：平台用户（系统管理员），关联平台租户(tenant_id=1)
     * - user_type = 1：租户用户（租户管理员、普通员工），关联具体租户
     * 
     * 登录流程：
     * 1. 通过账号查询用户信息
     * 2. 根据 user_type 判断用户类型
     * 3. 租户用户需要验证租户状态（是否存在、是否禁用、是否过期）
     * 4. 检查登录安全（是否被锁定）
     * 5. 根据登录方式验证用户身份
     * 6. 检查用户状态（是否被禁用）
     * 7. 记录登录历史
     * 8. 生成访问令牌和刷新令牌
     * 
     * @param req 登录请求参数
     * @return 登录响应，包含Token和用户信息
     * @throws BizException 登录失败时抛出业务异常
     */
    @Override
    public AuthResponseVo.LoginResponse login(AuthVo.LoginReq req) {
        log.info("开始用户登录, loginType={}, username={}, entryType={}", 
                req.loginType(), req.username(), req.entryType());
        
        // 1. 通过账号查询用户信息（使用认证专用方法，跳过租户拦截器）
        UserEntity user = null;
        if ("SCAN_CODE".equals(req.loginType())) {
            // 扫码登录：通过qrToken获取用户
            log.debug("使用扫码登录方式, qrToken={}", req.qrToken());
            String status = scanLoginService.getLoginStatus(req.qrToken());
            if (!"CONFIRMED".equals(status)) {
                log.warn("扫码登录未确认或已过期, qrToken={}, status={}", req.qrToken(), status);
                throw new BizException(ErrorCode.BAD_REQUEST, "扫码登录未确认或已过期");
            }
            String[] userInfo = scanLoginService.getUserInfo(req.qrToken());
            if (userInfo != null && userInfo.length >= 3) {
                // 使用认证专用查询方法，跳过租户拦截器
                user = userMapper.selectByIdForAuth(Long.parseLong(userInfo[0]));
                log.debug("扫码登录获取用户信息成功, userId={}", userInfo[0]);
            } else {
                log.warn("扫码登录信息不完整, qrToken={}", req.qrToken());
                throw new BizException(ErrorCode.BAD_REQUEST, "扫码登录信息不完整");
            }
        } else {
            // 账号密码/验证码登录：使用认证专用查询方法，跳过租户拦截器
            user = userMapper.selectByUsernameForAuth(req.username());
            if (user == null) {
                log.warn("登录失败，账号不存在, username={}", req.username());
                // 记录登录失败历史（账号不存在时无法获取userId，使用tenantId代替）
                recordLoginHistoryFailed(req, "账号不存在");
                throw new BizException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
            }
            log.debug("找到用户, userId={}, tenantId={}, userType={}", user.getId(), user.getTenantId(), user.getUserType());
            validateLogin(req.loginType(), req, user);
        }

        if (user == null) {
            log.error("用户对象为null，这是不应该发生的情况");
            recordLoginHistoryFailed(req, "用户对象为空");
            throw new BizException(ErrorCode.BAD_REQUEST, "用户不存在");
        }

        // 2. 根据用户类型判断是否需要验证租户状态
        TenantEntity tenant = null;
        boolean isPlatformUser = user.getUserType() != null && user.getUserType() == 0;
        String companyName = null;
        String companyLogoUrl = null;
        
        if (user.getTenantId() != null) {
            // sys_tenant 表在白名单中，不受租户拦截器影响
            tenant = tenantMapper.selectById(user.getTenantId());
            if (tenant == null) {
                log.warn("登录失败，用户所属租户不存在, userId={}, tenantId={}", user.getId(), user.getTenantId());
                recordLoginHistory(user, tenant, req, false, "租户不存在");
                throw new BizException(ErrorCode.NOT_FOUND, "用户所属租户不存在");
            }

            companyName = tenant.getTenantName();
            companyLogoUrl = tenant.getLogoUrl();
            // 租户用户需要验证租户状态
            if (!isPlatformUser) {
                if (tenant.getStatus() != 0) {
                    log.warn("登录失败，租户已被禁用, tenantCode={}, status={}", tenant.getShortCode(), tenant.getStatus());
                    recordLoginHistory(user, tenant, req, false, "租户已被禁用");
                    throw new BizException(ErrorCode.TENANT_FROZEN, "租户已被禁用，请联系管理员");
                }
                if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
                    log.warn("登录失败，租户已过期, tenantCode={}, expireTime={}", tenant.getShortCode(), tenant.getExpireTime());
                    recordLoginHistory(user, tenant, req, false, "租户已过期");
                    throw new BizException(ErrorCode.TENANT_EXPIRED, "租户已过期，请联系管理员");
                }
                log.debug("租户验证通过, tenantId={}, tenantName={}", tenant.getId(), tenant.getTenantName());
            } else {
                log.debug("平台用户登录，跳过租户状态验证");
            }
        }

        // 3. 检查登录安全
        String tenantCode = tenant != null ? tenant.getShortCode() : null;
        loginSecurityService.checkLoginAttempt(req.username(), tenantCode);

        // 4. 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 1) {
            log.warn("登录失败，用户已被禁用, userId={}, username={}", user.getId(), user.getUsername());
            recordLoginHistory(user, tenant, req, false, "用户已被禁用");
            throw new BizException(ErrorCode.USER_DISABLED, "账号已被禁用，请联系管理员");
        }

        // 5. 记录登录成功
        loginSecurityService.recordLoginAttempt(req.username(), tenantCode, true);
        if (tenant != null && !isPlatformUser) {
            updateRecentTenant(user.getId(), tenant.getId());
        }
        
        // 5.1 更新用户登录信息（登录次数和最后登录时间）
        updateLoginInfo(user.getId(), req.ipAddress());
        
        // 5.2 记录登录历史
        recordLoginHistory(user, tenant, req, true, null);
        
        log.debug("登录安全检查和历史记录更新完成");

        // 6. 生成令牌
        Long tenantId = user.getTenantId();
        List<String> roles = resolveUserRoles(user.getId(), tenantId, user.getUserType());
        String accessToken = jwtTokenService.createToken(user.getId(), tenantId, user.getUsername(), roles, user.getUserType());
        String refreshToken = refreshTokenService.create(user.getId(), tenantId);
        
        // 7. 确定 entryType
        String entryType = req.entryType();
        if (entryType == null || entryType.isEmpty()) {
            if (isPlatformUser) {
                entryType = "SYSTEM";
            } else {
                // 根据用户类型判断入口类型（userType=0为平台用户，已在上方处理）
                // 租户用户统一为 TENANT_ADMIN，具体权限由角色菜单控制
                entryType = "TENANT_ADMIN";
            }
        }
        
        log.info("用户登录成功, userId={}, username={}, tenantId={}, userType={}, entryType={}, roles={}", 
                user.getId(), user.getUsername(), tenantId, user.getUserType(), entryType, roles);

        // 生成通信会话密钥
        String sessionKey = sessionKeyService.create(user.getId(), tenantId);

        return new AuthResponseVo.LoginResponse(
                accessToken,
                refreshToken,
                7200,
                new AuthResponseVo.LoginUserInfo(
                        user.getId(),
                        tenantId,
                        user.getUsername(),
                        user.getRealName(),
                        user.getAvatarUrl(),
                        roles,
                        entryType,
                        user.getUserType(),
                        companyName,
                        FileUploadUtils.getResourceUrl(companyLogoUrl, "logo.png"),
                        tenant != null ? tenant.getWelcomeText() : null
                ),
                sessionKey
        );
    }

    private void validateLogin(String loginType, AuthVo.LoginReq payload, UserEntity user) {
        switch (loginType) {
            case "PASSWORD" -> validatePassword(payload.password(), user);
            case "SMS_CODE" -> validateSmsCode(payload.phone(), payload.smsCode(), user);
            case "EMAIL_CODE" -> validateEmailCode(payload.email(), payload.emailCode(), user);
            default -> throw new BizException(ErrorCode.BAD_REQUEST, "不支持的登录方式");
        }
    }

    private void validatePassword(String password, UserEntity user) {
        if (!userService.checkPassword(password, user.getPasswordHash())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "密码错误");
        }
    }

    private void validateSmsCode(String phone, String smsCode, UserEntity user) {
        if (!phone.equals(user.getPhone())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "手机号与用户不匹配");
        }
        verificationCodeService.validateCode("sms:code:" + phone + ":LOGIN", smsCode);
    }

    private void validateEmailCode(String email, String emailCode, UserEntity user) {
        if (!email.equals(user.getEmail())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "邮箱与用户不匹配");
        }
        verificationCodeService.validateCode("email:code:" + email + ":LOGIN", emailCode);
    }

    private void updateRecentTenant(Long userId, Long tenantId) {
        // 查找现有记录
        UserRecentTenantEntity existing = userRecentTenantMapper.selectOne(
                new LambdaQueryWrapper<UserRecentTenantEntity>()
                        .eq(UserRecentTenantEntity::getUserId, userId)
                        .eq(UserRecentTenantEntity::getTenantId, tenantId)
        );
        
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            // 更新现有记录
            existing.setLastLoginTime(now);
            existing.setLoginCount(existing.getLoginCount() + 1);
            existing.setUpdatedAt(now);
            userRecentTenantMapper.updateById(existing);
        } else {
            // 创建新记录
            UserRecentTenantEntity newRecord = new UserRecentTenantEntity();
            newRecord.setUserId(userId);
            newRecord.setTenantId(tenantId);
            newRecord.setLastLoginTime(now);
            newRecord.setLoginCount(1);
            newRecord.setCreatedAt(now);
            newRecord.setUpdatedAt(now);
            userRecentTenantMapper.insert(newRecord);
        }
    }
    
    /**
     * 更新用户登录信息
     * 包括登录次数、最后登录时间、最后登录IP
     * 
     * @param userId 用户ID
     * @param ipAddress 登录IP地址
     */
    private void updateLoginInfo(Long userId, String ipAddress) {
        try {
            log.info("开始更新用户登录信息, userId={}, ipAddress={}", userId, ipAddress);
            
            UserEntity user = userMapper.selectByIdForAuth(userId);
            if (user == null) {
                log.warn("更新登录信息失败，用户不存在, userId={}", userId);
                return;
            }
            
            LocalDateTime now = LocalDateTime.now();
            Integer oldLoginCount = user.getLoginCount();
            Integer newLoginCount = oldLoginCount == null ? 1 : oldLoginCount + 1;
            
            // 使用专门的更新方法，跳过租户拦截器
            int updated = userMapper.updateLoginInfo(userId, now, newLoginCount, ipAddress);
            
            log.info("更新用户登录信息成功, userId={}, loginCount: {} -> {}, lastLoginAt={}, lastLoginIp={}, 影响行数={}", 
                    userId, oldLoginCount, newLoginCount, now, ipAddress, updated);
        } catch (Exception e) {
            log.error("更新用户登录信息失败, userId={}, ipAddress={}", userId, ipAddress, e);
            // 不抛出异常，登录信息更新失败不影响登录流程
        }
    }
    
    /**
     * 记录登录历史
     * 系统管理员和租户用户都会记录登录历史，tenant_id 字段记录用户所属租户
     * @param user 用户实体
     * @param tenant 租户实体（系统管理员为平台租户，租户用户为对应租户）
     * @param req 登录请求
     * @param success 是否成功
     * @param failReason 失败原因
     */
    private void recordLoginHistory(UserEntity user, TenantEntity tenant, 
                                    AuthVo.LoginReq req, boolean success, String failReason) {
        try {
            LoginHistoryEntity history = new LoginHistoryEntity();
            // tenant_id: 系统管理员记录平台租户ID，租户用户记录对应租户ID
            history.setTenantId(user.getTenantId());
            history.setUserId(user.getId());
            history.setLoginType(req.loginType());
            history.setLoginTime(LocalDateTime.now());
            history.setLoginIp(req.ipAddress());
            history.setDeviceInfo(req.deviceInfo());
            history.setUserAgent(req.userAgent());
            history.setLoginStatus(success ? 1 : 0);
            history.setFailReason(failReason);
            history.setCreatedAt(LocalDateTime.now());
            
            loginHistoryMapper.insert(history);
            log.info("登录历史记录成功, userId={}, tenantId={}, loginType={}, status={}", 
                    user.getId(), user.getTenantId(), req.loginType(), success ? "成功" : "失败");
        } catch (Exception e) {
            log.error("登录历史记录失败, userId={}, 异常信息={}", user.getId(), e.getMessage(), e);
            // 不抛出异常，登录历史记录失败不影响登录流程
        }
    }
    
    /**
     * 记录登录失败历史（账号不存在时使用）
     * 当账号不存在时，无法获取userId，使用username查询tenant_id
     * @param req 登录请求
     * @param failReason 失败原因
     */
    private void recordLoginHistoryFailed(AuthVo.LoginReq req, String failReason) {
        try {
            // 尝试通过username查询tenant_id（使用认证专用方法）
            UserEntity tempUser = userMapper.selectByUsernameForAuth(req.username());
            Long tenantId = tempUser != null ? tempUser.getTenantId() : null;
            
            LoginHistoryEntity history = new LoginHistoryEntity();
            history.setTenantId(tenantId);
            history.setUserId(null); // 账号不存在，userId为null
            history.setLoginType(req.loginType());
            history.setLoginTime(LocalDateTime.now());
            history.setLoginIp(req.ipAddress());
            history.setDeviceInfo(req.deviceInfo());
            history.setUserAgent(req.userAgent());
            history.setLoginStatus(0); // 失败
            history.setFailReason(failReason);
            history.setCreatedAt(LocalDateTime.now());
            
            loginHistoryMapper.insert(history);
            log.debug("登录失败历史记录成功, username={}, reason={}", req.username(), failReason);
        } catch (Exception e) {
            log.error("登录失败历史记录异常, username={}", req.username(), e);
            // 不抛出异常
        }
    }

    @Override
    public void sendSmsCode(String phone, String tenantCode, String scene) {
        verificationCodeService.sendSmsCode(phone, scene);
    }

    @Override
    public void sendEmailCode(String email, String tenantCode, String scene) {
        verificationCodeService.sendEmailCode(email, scene);
    }

    /**
     * 刷新访问令牌
     * 使用有效的RefreshToken换取新的AccessToken
     * 
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌
     * @throws BizException Token无效或用户不存在时抛出异常
     */
    @Override
    public String refreshToken(String refreshToken) {
        log.debug("开始刷新Token");
        
        // 验证RefreshToken
        String session = refreshTokenService.validate(refreshToken);
        if (session == null) {
            log.warn("Token刷新失败，RefreshToken无效或已过期");
            throw new BizException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        
        // 解析Token中的用户信息
        String[] parts = session.split(":");
        Long userId = Long.parseLong(parts[0]);
        Long tenantId = Long.parseLong(parts[1]);
        log.debug("RefreshToken验证通过, userId={}, tenantId={}", userId, tenantId);

        // 验证用户是否存在且有效（使用认证专用查询方法，跳过租户拦截器）
        UserEntity user = userMapper.selectByIdForAuth(userId);
        if (user == null) {
            log.warn("Token刷新失败，用户不存在, userId={}", userId);
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在或已被禁用");
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            log.warn("Token刷新失败，用户已被禁用, userId={}", userId);
            throw new BizException(ErrorCode.USER_DISABLED);
        }

        // 生成新的AccessToken
        List<String> roles = resolveUserRoles(user.getId(), tenantId, user.getUserType());
        String newAccessToken = jwtTokenService.createToken(user.getId(), tenantId, user.getUsername(), roles, user.getUserType());
        
        log.info("Token刷新成功, userId={}, username={}", userId, user.getUsername());
        return newAccessToken;
    }

    @Override
    public void resetPassword(AuthVo.ResetPasswordReq payload) {
        // sys_tenant 表在白名单中，不受租户拦截器影响
        TenantEntity tenant = tenantMapper.selectOne(
                new LambdaQueryWrapper<TenantEntity>()
                        .eq(TenantEntity::getShortCode, payload.tenantCode())
                        .eq(TenantEntity::getStatus, 0)
        );
        if (tenant == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "租户不存在或已禁用");
        }

        // 使用认证专用查询方法，跳过租户拦截器
        UserEntity user = userMapper.selectByPhoneForAuth(payload.phone(), tenant.getId());
        if (user == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户不存在");
        }

        verificationCodeService.validateCode("sms:code:" + payload.phone() + ":RESET_PASSWORD", payload.smsCode());

        userService.updatePassword(user.getId(), payload.newPassword());
    }

    @Override
    public void verifySmsCodeForForgotPassword(AuthVo.VerifySmsCodeReq payload) {
        // 根据手机号查询租户
        TenantEntity tenant = null;
        if (payload.tenantCode() != null && !payload.tenantCode().isBlank()) {
            tenant = tenantMapper.selectOne(
                    new LambdaQueryWrapper<TenantEntity>()
                            .eq(TenantEntity::getShortCode, payload.tenantCode())
                            .eq(TenantEntity::getStatus, 0)
            );
        } else {
            // 根据手机号查询租户
            List<TenantEntity> tenants = tenantMapper.selectList(
                    new LambdaQueryWrapper<TenantEntity>()
                            .eq(TenantEntity::getStatus, 0)
            );
            for (TenantEntity t : tenants) {
                UserEntity u = userMapper.selectByPhoneForAuth(payload.phone(), t.getId());
                if (u != null) {
                    tenant = t;
                    break;
                }
            }
        }
        
        if (tenant == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "该手机号未绑定任何租户");
        }

        // 验证用户是否存在
        UserEntity user = userMapper.selectByPhoneForAuth(payload.phone(), tenant.getId());
        if (user == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户不存在");
        }

        // 验证短信验证码
        verificationCodeService.validateCode("sms:code:" + payload.phone() + ":RESET_PASSWORD", payload.smsCode());
    }

    @Override
    public AuthResponseVo.UserInfoResult getCurrentUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户不存在");
        }

        // 获取用户权限（菜单权限标识）
        List<String> permissions = permissionMapper.selectPerms(userId, user.getTenantId());

        // 获取租户信息（公司信息）
        String companyName = null;
        String companyShortCode = null;
        String companyContact = null;
        String companyPhone = null;
        String companyLogoUrl = null;
        
        if (user.getTenantId() != null) {
            TenantEntity tenant = tenantMapper.selectById(user.getTenantId());
            if (tenant != null) {
                companyName = tenant.getTenantName();
                companyShortCode = tenant.getShortCode();
                companyContact = tenant.getContactName();
                companyPhone = tenant.getPhone();
                companyLogoUrl = tenant.getLogoUrl();
            }
        }

        // 获取部门信息
        String deptName = null;
        if (user.getDeptId() != null) {
            DeptEntity dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) {
                deptName = dept.getDeptName();
            }
        }

        // 获取直属领导信息
        String leaderName = null;
        if (user.getLeaderUserId() != null) {
            UserEntity leader = userMapper.selectById(user.getLeaderUserId());
            if (leader != null) {
                leaderName = leader.getRealName();
            }
        }

        // 格式化最后登录时间
        String lastLoginAt = user.getLastLoginAt() != null 
            ? user.getLastLoginAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            : null;

        return new AuthResponseVo.UserInfoResult(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getPhone(),
                user.getEmail(),
                FileUploadUtils.getResourceUrl(user.getAvatarUrl(), "avatar.jpg"),
                user.getTenantId(),
                user.getUserType(),
                permissions,
                companyName,
                companyShortCode,
                companyContact,
                companyPhone,
                FileUploadUtils.getResourceUrl(companyLogoUrl, "logo.png"),
                deptName,
                user.getPosition(),
                user.getLeaderUserId(),
                leaderName,
                lastLoginAt
        );
    }

    @Override
    public void updateCurrentUser(Long userId, AuthVo.UpdateUserReq payload) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户不存在");
        }

        // 更新用户信息
        if (payload.realName() != null) {
            user.setRealName(payload.realName());
        }
        if (payload.phone() != null) {
            user.setPhone(payload.phone());
        }
        if (payload.email() != null) {
            user.setEmail(payload.email());
        }
        if (payload.avatarUrl() != null) {
            user.setAvatarUrl(payload.avatarUrl());
        }

        userMapper.updateById(user);
        log.info("用户信息更新成功, userId={}", userId);
    }

    @Override
    public void changePassword(Long userId, AuthVo.ChangePasswordReq payload) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户不存在");
        }

        // 验证旧密码 - 使用 UserService 的统一方法
        if (!userService.checkPassword(payload.oldPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "当前密码错误");
        }

        // 更新密码 - 使用 UserService 的统一方法
        userService.updatePassword(userId, payload.newPassword());
        log.info("用户密码修改成功, userId={}", userId);
    }
}
