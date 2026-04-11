package com.plating.erp.auth;

import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.common.security.JwtTokenService;
import com.plating.erp.common.security.RefreshTokenService;
import com.plating.erp.common.tenant.TenantContextHolder;
import com.plating.erp.auth.vo.AuthVo;
import com.plating.erp.auth.vo.AuthResponseVo;
import jakarta.validation.Valid;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.service.UserService;
import com.plating.erp.platform.entity.TenantEntity;
import com.plating.erp.platform.service.TenantService;
import com.plating.erp.common.security.PermissionMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserService userService;
    private final TenantService tenantService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final PermissionMapper permissionMapper;

    public AuthController(UserService userService, TenantService tenantService, JwtTokenService jwtTokenService, RefreshTokenService refreshTokenService, PasswordEncoder passwordEncoder, PermissionMapper permissionMapper) {
        this.userService = userService;
        this.tenantService = tenantService;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.permissionMapper = permissionMapper;
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody AuthVo.LoginReq payload) {
        String tenantCode = payload.tenantCode() == null ? "" : payload.tenantCode();
        String username = payload.username() == null ? "" : payload.username();
        String password = payload.password() == null ? "" : payload.password();
        TenantEntity tenant = tenantService.getByShortCode(tenantCode);
        if (tenant == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "租户不存在");
        }
        if (tenant.getStatus() != null && tenant.getStatus() == 1) {
            throw new BizException(ErrorCode.TENANT_FROZEN);
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.TENANT_EXPIRED);
        }
        UserEntity user;
        try {
            TenantContextHolder.setTenantId(tenant.getId());
            user = userService.findByUsernameAndTenantId(username, tenant.getId());
        } finally {
            TenantContextHolder.clear();
        }
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号不存在");
        }
        boolean passwordValid = passwordEncoder.matches(password, user.getPasswordHash()) || password.equals(user.getPasswordHash());
        if (!passwordValid) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }
        List<String> roles = permissionMapper.selectRoleKeys(user.getId(), user.getTenantId());
        if (roles == null || roles.isEmpty()) {
            roles = "system".equalsIgnoreCase(user.getUsername()) ? List.of("system") : List.of("TENANT_ADMIN");
        }
        String accessToken = jwtTokenService.createToken(user.getId(), user.getTenantId(), user.getUsername(), roles);
        String refreshToken = refreshTokenService.create(user.getId(), user.getTenantId());
        return ApiResponse.ok(new AuthResponseVo.LoginResponse(
                accessToken,
                refreshToken,
                7200,
                new AuthResponseVo.LoginUserInfo(
                        user.getId(),
                        user.getTenantId(),
                        user.getUsername(),
                        user.getRealName(),
                        roles
                )
        ));
    }

    @PostMapping("/sms-code")
    public ApiResponse<?> smsCode(@Valid @RequestBody AuthVo.SmsCodeReq payload) {
        return ApiResponse.ok(new AuthResponseVo.CodeSendResponse("SMS", payload.phone() == null ? "" : payload.phone(), true));
    }

    @PostMapping("/email-code")
    public ApiResponse<?> emailCode(@Valid @RequestBody AuthVo.EmailCodeReq payload) {
        return ApiResponse.ok(new AuthResponseVo.CodeSendResponse("EMAIL", payload.email() == null ? "" : payload.email(), true));
    }

    @PostMapping("/scan-ticket")
    public ApiResponse<?> scanTicket() {
        return ApiResponse.ok(new AuthResponseVo.ScanTicketResponse("qr_mock_001", 120));
    }

    @PostMapping("/scan-confirm")
    public ApiResponse<?> scanConfirm(@Valid @RequestBody AuthVo.ScanConfirmReq payload) {
        return ApiResponse.ok(new AuthResponseVo.ScanConfirmResponse(payload.qrToken() == null ? "qr_mock_001" : payload.qrToken(), true));
    }

    @PostMapping("/refresh")
    public ApiResponse<?> refresh(@Valid @RequestBody AuthVo.RefreshReq req) {
        String session = refreshTokenService.validate(req.refreshToken());
        if (session == null) {
            throw new BizException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        String[] parts = session.split(":");
        Long userId = Long.parseLong(parts[0]);
        Long tenantId = Long.parseLong(parts[1]);
        UserEntity user = userService.getById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        List<String> roles = permissionMapper.selectRoleKeys(user.getId(), user.getTenantId());
        if (roles == null || roles.isEmpty()) {
            roles = "system".equalsIgnoreCase(user.getUsername()) ? List.of("system") : List.of("TENANT_ADMIN");
        }
        String accessToken = jwtTokenService.createToken(userId, tenantId, user.getUsername(), roles);
        return ApiResponse.ok(new CommonResponses.TokenResponse(accessToken, 7200));
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(@RequestBody(required = false) AuthVo.LogoutReq req) {
        if (req != null) {
            refreshTokenService.invalidate(req.refreshToken());
        }
        return ApiResponse.ok(new CommonResponses.SuccessResponse(true));
    }
}
