package com.plating.erp.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.auth.entity.LoginHistoryEntity;
import com.plating.erp.auth.mapper.LoginHistoryMapper;
import com.plating.erp.auth.service.AuthService;
import com.plating.erp.auth.service.LoginSecurityService;
import com.plating.erp.auth.service.ScanLoginService;
import com.plating.erp.auth.vo.AuthResponseVo;
import com.plating.erp.auth.vo.AuthVo;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.JwtTokenService;
import com.plating.erp.common.security.RefreshTokenService;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.common.security.SessionKeyService;
import com.plating.erp.common.util.IpUtils;
import com.plating.erp.iam.service.MenuService;
import com.plating.erp.iam.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 认证控制器
 * 处理用户登录、登出、Token刷新、验证码发送等认证相关请求
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final LoginHistoryMapper loginHistoryMapper;
    private final ScanLoginService scanLoginService;
    private final LoginSecurityService loginSecurityService;
    private final UserService userService;
    private final MenuService menuService;
    private final SessionKeyService sessionKeyService;

    public AuthController(AuthService authService, JwtTokenService jwtTokenService,
                         RefreshTokenService refreshTokenService, LoginHistoryMapper loginHistoryMapper,
                         ScanLoginService scanLoginService, LoginSecurityService loginSecurityService,
                         UserService userService, MenuService menuService,
                         SessionKeyService sessionKeyService) {
        this.authService = authService;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.loginHistoryMapper = loginHistoryMapper;
        this.scanLoginService = scanLoginService;
        this.loginSecurityService = loginSecurityService;
        this.userService = userService;
        this.menuService = menuService;
        this.sessionKeyService = sessionKeyService;
    }

    /**
     * 搜索租户
     * @param keyword 搜索关键词（租户编码或名称）
     * @param limit 返回结果数量限制
     * @return 租户搜索列表
     */
    @GetMapping("/tenants/search")
    public ApiResponse<?> searchTenants(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        log.debug("搜索租户, keyword={}, limit={}", keyword, limit);
        List<AuthResponseVo.TenantSearchResult> results = authService.searchTenants(keyword, limit);
        log.info("租户搜索完成, keyword={}, 结果数={}", keyword, results.size());
        return ApiResponse.ok(results);
    }

    /**
     * 根据用户名查询租户信息
     * 用于登录时自动反显用户所属企业
     * @param username 用户名
     * @return 用户所属租户信息
     */
    @GetMapping("/tenants/by-username")
    public ApiResponse<?> getTenantByUsername(@RequestParam String username) {
        log.debug("根据用户名查询租户, username={}", username);
        AuthResponseVo.TenantByUsernameResult result = authService.getTenantByUsername(username);
        if (result.found()) {
            log.info("找到用户所属租户, username={}, tenant={}", username, result.tenantName());
        } else {
            log.warn("未找到用户所属租户, username={}", username);
        }
        return ApiResponse.ok(result);
    }

    /**
     * 根据手机号查询租户信息
     * 用于忘记密码时自动获取用户所属租户
     * @param phone 手机号
     * @return 手机号所属租户信息
     */
    @GetMapping("/tenants/by-phone")
    public ApiResponse<?> getTenantByPhone(@RequestParam String phone) {
        log.debug("根据手机号查询租户, phone={}", phone);
        AuthResponseVo.TenantByPhoneResult result = authService.getTenantByPhone(phone);
        if (result.found()) {
            log.info("找到手机号所属租户, phone={}, tenant={}", phone, result.tenantName());
        } else {
            log.warn("未找到手机号所属租户, phone={}", phone);
        }
        return ApiResponse.ok(result);
    }

    /**
     * 获取用户最近登录的租户列表
     * @param request HTTP请求，用于获取当前用户Token
     * @return 最近登录的租户列表
     */
    @GetMapping("/tenants/recent")
    public ApiResponse<?> getRecentTenants(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        log.debug("获取最近登录租户列表");
        List<AuthResponseVo.RecentTenantResult> results = authService.getRecentTenants(auth);
        log.info("获取最近登录租户完成, 结果数={}", results.size());
        return ApiResponse.ok(results);
    }

    /**
     * 用户登录
     * 支持多种登录方式：密码、短信验证码、邮箱验证码、扫码登录
     * 
     * 登录流程：
     * 1. 检查IP暴力破解防护
     * 2. 验证租户状态（是否存在、是否过期）
     * 3. 检查登录安全（是否被锁定）
     * 4. 根据登录方式验证用户身份
     * 5. 检查用户状态（是否被禁用）
     * 6. 记录登录历史
     * 7. 生成访问令牌和刷新令牌
     * 
     * @param payload 登录请求参数
     * @param request HTTP请求，用于获取客户端IP
     * @return 登录成功后的Token和用户信息
     */
    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody AuthVo.LoginReq payload, HttpServletRequest request) {
        String clientIp = IpUtils.getClientIp(request);
        log.info("用户登录请求, loginType={}, username={}, entryType={}, clientIp={}",
                payload.loginType(), payload.username(), payload.entryType(), clientIp);
        
        // 将IP地址设置到payload中
        payload = new AuthVo.LoginReq(
                payload.loginType(),
                payload.entryType(),
                payload.tenantCode(),
                payload.username(),
                payload.password(),
                payload.phone(),
                payload.smsCode(),
                payload.email(),
                payload.emailCode(),
                payload.qrToken(),
                payload.clientType(),
                payload.rememberTenant(),
                clientIp,
                payload.deviceInfo(),
                payload.userAgent()
        );
        
        try {
            // 1. 检查IP暴力破解防护
            loginSecurityService.checkBruteForce(clientIp);
            
            AuthResponseVo.LoginResponse response = authService.login(payload);
            
            // 记录成功的IP尝试
            loginSecurityService.recordBruteForceAttempt(clientIp, true);
            
            log.info("用户登录成功, username={}, tenantCode={}, clientIp={}", 
                    payload.username(), payload.tenantCode(), clientIp);
            return ApiResponse.ok(response);
        } catch (Exception e) {
            // 记录失败的IP尝试
            loginSecurityService.recordBruteForceAttempt(clientIp, false);
            log.error("用户登录失败, username={}, clientIp={}, error={}", 
                    payload.username(), clientIp, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 获取客户端真实IP地址
     * 考虑反向代理的情况，优先从X-Forwarded-For头获取
     * 
     * @param request HTTP请求
     * @return 客户端IP地址
     */

    /**
     * 发送短信验证码
     * @param payload 短信验证码请求参数
     * @return 发送结果
     */
    @PostMapping("/sms-code")
    public ApiResponse<?> smsCode(@Valid @RequestBody AuthVo.SmsCodeReq payload) {
        log.info("发送短信验证码, phone={}, scene={}", payload.phone(), payload.scene());
        authService.sendSmsCode(payload.phone(), payload.tenantCode(), payload.scene());
        log.info("短信验证码发送成功, phone={}", payload.phone());
        return ApiResponse.ok(new AuthResponseVo.CodeSendResponse("SMS", payload.phone() == null ? "" : payload.phone(), true));
    }

    /**
     * 发送邮箱验证码
     * @param payload 邮箱验证码请求参数
     * @return 发送结果
     */
    @PostMapping("/email-code")
    public ApiResponse<?> emailCode(@Valid @RequestBody AuthVo.EmailCodeReq payload) {
        log.info("发送邮箱验证码, email={}, scene={}", payload.email(), payload.scene());
        authService.sendEmailCode(payload.email(), payload.tenantCode(), payload.scene());
        log.info("邮箱验证码发送成功, email={}", payload.email());
        return ApiResponse.ok(new AuthResponseVo.CodeSendResponse("EMAIL", payload.email() == null ? "" : payload.email(), true));
    }

    /**
     * 生成扫码登录二维码票据
     * @param payload 客户端类型
     * @return 二维码Token和Base64图片
     */
    @PostMapping("/scan-ticket")
    public ApiResponse<?> scanTicket(@Valid @RequestBody AuthVo.ScanTicketReq payload) {
        log.info("生成扫码登录二维码, clientType={}", payload.clientType());
        String[] result = scanLoginService.generateQrTicket();
        String qrToken = result[0];
        String qrImageBase64 = result[1];
        log.info("扫码登录二维码生成成功, qrToken={}", qrToken);
        return ApiResponse.ok(new AuthResponseVo.ScanTicketResponse(qrToken, qrImageBase64, 120));
    }

    /**
     * 扫码（移动端扫码）
     * 移动端扫描二维码后调用此接口，标记二维码为已扫码状态
     * @param payload 扫码参数
     * @return 扫码结果
     */
    @PostMapping("/scan")
    public ApiResponse<?> scan(@Valid @RequestBody AuthVo.ScanReq payload) {
        log.info("移动端扫码, qrToken={}, userId={}", payload.qrToken(), payload.userId());
        scanLoginService.scanTicket(payload.qrToken(), payload.userId());
        log.info("移动端扫码成功, qrToken={}, userId={}", payload.qrToken(), payload.userId());
        return ApiResponse.ok(new AuthResponseVo.ScanResponse(payload.qrToken(), true));
    }

    /**
     * 扫码登录确认
     * 移动端扫码后调用此接口确认登录
     * @param payload 扫码确认参数
     * @return 确认结果
     */
    @PostMapping("/scan-confirm")
    public ApiResponse<?> scanConfirm(@Valid @RequestBody AuthVo.ScanConfirmReq payload) {
        log.info("扫码登录确认, qrToken={}, confirm={}, userId={}",
                payload.qrToken(), payload.confirm(), payload.userId());
        if (payload.confirm() != null && !payload.confirm()) {
            scanLoginService.cleanup(payload.qrToken());
            log.info("扫码登录已取消, qrToken={}", payload.qrToken());
            return ApiResponse.ok(new AuthResponseVo.ScanConfirmResponse(payload.qrToken(), false));
        }
        scanLoginService.confirmLogin(payload.qrToken(), payload.userId(), payload.tenantId(), payload.username());
        log.info("扫码登录确认成功, qrToken={}, userId={}", payload.qrToken(), payload.userId());
        return ApiResponse.ok(new AuthResponseVo.ScanConfirmResponse(payload.qrToken(), true));
    }

    /**
     * 查询扫码登录状态
     * 前端轮询此接口获取扫码登录状态
     * @param qrToken 二维码Token
     * @return 登录状态和Token信息（已确认时）
     */
    @GetMapping("/scan-status")
    public ApiResponse<?> scanStatus(@RequestParam String qrToken) {
        log.debug("查询扫码登录状态, qrToken={}", qrToken);
        String status = scanLoginService.getLoginStatus(qrToken);
        
        // 如果已确认，调用标准登录流程生成 Token
        if ("CONFIRMED".equals(status)) {
            log.info("扫码登录已确认，调用标准登录流程, qrToken={}", qrToken);
            
            try {
                // 使用标准登录流程（包含角色解析、安全校验、Token生成等完整逻辑）
                // LoginReq 字段顺序: loginType, entryType, tenantCode, username, password, 
                // phone, smsCode, email, emailCode, qrToken, clientType, rememberTenant, ipAddress, deviceInfo, userAgent
                // entryType 传 null，让后端根据用户角色自动判断 (SYSTEM/TENANT_ADMIN/EMPLOYEE)
                AuthVo.LoginReq loginReq = new AuthVo.LoginReq(
                        "SCAN_CODE",    // loginType
                        null,           // entryType (让后端根据角色自动判断)
                        null,           // tenantCode
                        null,           // username (扫码登录不需要)
                        null,           // password
                        null,           // phone
                        null,           // smsCode
                        null,           // email
                        null,           // emailCode
                        qrToken,        // qrToken
                        "WEB",          // clientType
                        null,           // rememberTenant
                        null,           // ipAddress (后端自动获取)
                        null,           // deviceInfo (扫码登录不传递)
                        null            // userAgent (扫码登录不传递)
                );
                
                // 调用标准登录方法
                AuthResponseVo.LoginResponse loginResponse = authService.login(loginReq);
                
                // 清理扫码数据
                scanLoginService.cleanup(qrToken);
                
                log.info("扫码登录成功, userId={}, username={}", 
                        loginResponse.userInfo().userId(), 
                        loginResponse.userInfo().username());
                
                return ApiResponse.ok(new AuthResponseVo.ScanStatusResponse(
                        status,
                        loginResponse.accessToken(),
                        loginResponse.refreshToken(),
                        loginResponse.expiresIn(),
                        loginResponse.userInfo(),
                        loginResponse.sessionKey()
                ));
            } catch (Exception e) {
                log.error("扫码登录失败, qrToken={}", qrToken, e);
                scanLoginService.cleanup(qrToken);
                throw e;
            }
        }
        
        return ApiResponse.ok(new AuthResponseVo.ScanStatusResponse(
                status,
                null,
                null,
                7200,
                null,
                null
        ));
    }

    /**
     * 刷新访问令牌
     * 使用RefreshToken换取新的AccessToken
     * @param req 刷新Token请求
     * @return 新的AccessToken
     */
    @PostMapping("/refresh")
    public ApiResponse<?> refresh(@Valid @RequestBody AuthVo.RefreshReq req) {
        log.info("刷新Token请求");
        try {
            String accessToken = authService.refreshToken(req.refreshToken());
            log.info("Token刷新成功");
            return ApiResponse.ok(new CommonResponses.TokenResponse(accessToken, 7200));
        } catch (Exception e) {
            log.error("Token刷新失败, error={}", e.getMessage());
            throw e;
        }
    }

    /**
     * 用户登出
     * 使当前RefreshToken失效
     * @param req 登出请求（包含RefreshToken）
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ApiResponse<?> logout(@RequestBody(required = false) AuthVo.LogoutReq req) {
        log.info("用户登出请求");
        if (req != null && req.refreshToken() != null) {
            refreshTokenService.invalidate(req.refreshToken());
            log.info("用户登出成功, RefreshToken已失效");
        }
        // 清除通信会话密钥
        try {
            var user = SecurityUtils.currentUser();
            sessionKeyService.remove(user.userId(), user.tenantId());
        } catch (Exception ignored) {
        }
        return ApiResponse.ok(new CommonResponses.SuccessResponse(true));
    }

    /**
     * 重置密码
     * 通过手机号验证码重置用户密码
     * @param payload 重置密码请求参数
     * @return 重置结果
     */
    @PostMapping("/reset-password")
    public ApiResponse<?> resetPassword(@Valid @RequestBody AuthVo.ResetPasswordReq payload) {
        log.info("重置密码请求, phone={}", payload.phone());
        try {
            authService.resetPassword(payload);
            log.info("密码重置成功, phone={}", payload.phone());
            return ApiResponse.ok(new CommonResponses.SuccessResponse(true));
        } catch (Exception e) {
            log.error("密码重置失败, phone={}, error={}", payload.phone(), e.getMessage());
            throw e;
        }
    }

    /**
     * 验证短信验证码（忘记密码流程）
     * @param payload 验证请求
     * @return 验证结果
     */
    @PostMapping("/verify-sms-code")
    public ApiResponse<?> verifySmsCode(@Valid @RequestBody AuthVo.VerifySmsCodeReq payload) {
        log.info("验证短信验证码, phone={}", payload.phone());
        try {
            authService.verifySmsCodeForForgotPassword(payload);
            log.info("短信验证码验证成功, phone={}", payload.phone());
            return ApiResponse.ok(new CommonResponses.SuccessResponse(true));
        } catch (Exception e) {
            log.error("短信验证码验证失败, phone={}, error={}", payload.phone(), e.getMessage());
            throw e;
        }
    }

    /**
     * 获取用户登录历史记录
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param request HTTP请求
     * @return 登录历史分页列表
     */
    @GetMapping("/login-history")
    public ApiResponse<?> getLoginHistory(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request
    ) {
        log.debug("获取登录历史, pageNum={}, pageSize={}", pageNum, pageSize);
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            log.warn("获取登录历史失败, 未提供有效的Authorization头");
            return ApiResponse.ok(new PageResult<>(List.of(), 0L));
        }
        Long userId = jwtTokenService.getUserIdFromToken(auth.substring(7));
        if (userId == null) {
            log.warn("获取登录历史失败, 无法从Token解析用户ID");
            return ApiResponse.ok(new PageResult<>(List.of(), 0L));
        }
        LambdaQueryWrapper<LoginHistoryEntity> qw = new LambdaQueryWrapper<>();
        qw.eq(LoginHistoryEntity::getUserId, userId)
          .orderByDesc(LoginHistoryEntity::getLoginTime);
        Page<LoginHistoryEntity> page = loginHistoryMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        List<AuthResponseVo.LoginHistoryResult> records = page.getRecords().stream()
                .map(h -> new AuthResponseVo.LoginHistoryResult(
                        h.getId(),
                        h.getLoginType(),
                        h.getLoginTime(),
                        h.getLoginIp(),
                        h.getDeviceInfo(),
                        h.getLoginStatus()
                ))
                .toList();
        log.info("获取登录历史完成, userId={}, 记录数={}", userId, records.size());
        return ApiResponse.ok(new PageResult<>(records, page.getTotal()));
    }

    /**
     * 获取当前用户信息
     * @return 当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse<?> getCurrentUser() {
        var me = SecurityUtils.currentUser();
        log.debug("获取当前用户信息, userId={}", me.userId());
        AuthResponseVo.UserInfoResult result = authService.getCurrentUser(me.userId());
        log.info("获取当前用户信息成功, userId={}", result.id());
        return ApiResponse.ok(result);
    }

    /**
     * 更新当前用户信息
     * @param payload 用户信息
     * @return 操作结果
     */
    @PutMapping("/me")
    @AuditLog(module = "个人信息", operateType = "UPDATE", bizModule = "user", fieldName = "realName")
    public ApiResponse<?> updateCurrentUser(@Valid @RequestBody AuthVo.UpdateUserReq payload) {
        var me = SecurityUtils.currentUser();
        log.info("更新当前用户信息, userId={}", me.userId());
        authService.updateCurrentUser(me.userId(), payload);
        log.info("更新当前用户信息成功, userId={}", me.userId());
        return ApiResponse.ok("更新成功");
    }

    /**
     * 修改密码
     * @param payload 密码信息
     * @return 操作结果
     */
    @PostMapping("/change-password")
    @AuditLog(module = "个人信息", operateType = "CHANGE_PASSWORD", bizModule = "user", fieldName = "password")
    public ApiResponse<?> changePassword(@Valid @RequestBody AuthVo.ChangePasswordReq payload) {
        var me = SecurityUtils.currentUser();
        log.info("修改密码请求, userId={}", me.userId());
        authService.changePassword(me.userId(), payload);
        log.info("修改密码成功, userId={}", me.userId());
        return ApiResponse.ok("密码修改成功");
    }

    /**
     * 上传头像
     * @param avatarFile 头像文件
     * @return OSS路径（已URLEncode编码）
     */
    @PostMapping("/avatar/upload")
    @AuditLog(module = "个人信息", operateType = "UPLOAD_AVATAR", bizModule = "user", fieldName = "avatarUrl")
    public ApiResponse<String> uploadAvatar(@RequestParam("file") MultipartFile avatarFile) {
        var me = SecurityUtils.currentUser();
        log.info("上传头像请求, userId={}", me.userId());
        String ossPath = userService.updateAvatar(me.userId(), avatarFile);
        log.info("上传头像成功, userId={}, ossPath={}", me.userId(), ossPath);
        return ApiResponse.ok(ossPath);
    }

    /**
     * 发送绑定手机验证码
     * @param payload 包含新手机号
     * @return 发送结果
     */
    @PostMapping("/bind-phone/send-code")
    @AuditLog(module = "个人信息", operateType = "SEND_BIND_PHONE_CODE", bizModule = "user", fieldName = "phone")
    public ApiResponse<?> sendBindPhoneCode(@Valid @RequestBody AuthVo.BindPhoneSendCodeReq payload) {
        var me = SecurityUtils.currentUser();
        log.info("发送绑定手机验证码, userId={}, phone={}", me.userId(), payload.phone());
        authService.sendBindPhoneCode(me.userId(), payload.phone(),me.tenantId());
        log.info("绑定手机验证码发送成功, userId={}, phone={}", me.userId(), payload.phone());
        return ApiResponse.ok(new AuthResponseVo.CodeSendResponse("SMS", payload.phone(), true));
    }

    /**
     * 绑定手机号
     * 验证短信验证码后绑定新手机号
     * @param payload 包含新手机号和验证码
     * @return 操作结果
     */
    @PostMapping("/bind-phone")
    @AuditLog(module = "个人信息", operateType = "BIND_PHONE", bizModule = "user", fieldName = "phone")
    public ApiResponse<?> bindPhone(@Valid @RequestBody AuthVo.BindPhoneReq payload) {
        var me = SecurityUtils.currentUser();
        log.info("绑定手机号, userId={}, phone={}", me.userId(), payload.phone());
        authService.bindPhone(me.userId(), payload);
        log.info("手机号绑定成功, userId={}, phone={}", me.userId(), payload.phone());
        return ApiResponse.ok("手机号绑定成功");
    }

    /**
     * 获取用户动态路由和权限
     * 根据用户角色返回对应的菜单树和权限标识:
     * - 系统管理员: 返回所有平台级菜单 + 全部权限
     * - 租户管理员: 返回平台级 + 租户级菜单 + 本公司全部权限
     * - 普通员工: 返回角色关联的菜单 + 个人权限
     * 
     * @return 路由树和权限列表
     */
    @GetMapping("/routes")
    public ApiResponse<Map<String, Object>> getUserRoutes() {

        // 获取用户路由和权限
        Map<String, Object> result = menuService.getUserRoutesAndPermissions();
        return ApiResponse.ok(result);
    }
}
