package com.plating.erp.auth.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthVo {
    public record LoginReq(
            @NotBlank(message = "loginType不能为空") String loginType,
            String entryType,
            @Size(max = 16, message = "tenantCode长度不能超过16") String tenantCode,
            @Size(min = 2, max = 64, message = "username长度需在2-64") @Pattern(regexp = "^[a-zA-Z0-9:@._-]+$", message = "username格式不合法") String username,
            String password,
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
            @Size(min = 4, max = 8, message = "验证码长度不合法") String smsCode,
            @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", message = "邮箱格式不正确") String email,
            @Size(min = 4, max = 8, message = "验证码长度不合法") String emailCode,
            @Size(max = 128, message = "qrToken长度不能超过128") String qrToken,
            @NotBlank(message = "clientType不能为空") String clientType,
            Boolean rememberTenant,
            String ipAddress,  // 由后端从HTTP请求中自动获取，前端无需传递
            String deviceInfo, // 设备信息（如 Windows NT 10.0、iPhone等）
            String userAgent,  // 浏览器UA
            String rsaClientId // RSA 密钥对标识，用于解密前端加密的密码
    ) {
    }

    public record SmsCodeReq(
            @NotBlank(message = "phone不能为空") @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
            @Size(max = 16, message = "tenantCode长度不能超过16") String tenantCode,
            @NotBlank(message = "scene不能为空") String scene
    ) {
    }

    public record EmailCodeReq(
            @NotBlank(message = "email不能为空") @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", message = "邮箱格式不正确") String email,
            @Size(max = 16, message = "tenantCode长度不能超过16") String tenantCode,
            @NotBlank(message = "scene不能为空") String scene
    ) {
    }

    public record ScanTicketReq(
            @NotBlank(message = "clientType不能为空") String clientType
    ) {
    }

    public record ScanReq(
            @NotBlank(message = "qrToken不能为空") @Size(max = 128, message = "qrToken长度不能超过128") String qrToken,
            @NotBlank(message = "userId不能为空") Long userId
    ) {
    }

    public record ScanConfirmReq(
            @NotBlank(message = "qrToken不能为空") @Size(max = 128, message = "qrToken长度不能超过128") String qrToken,
            Boolean confirm,
            Long userId,
            Long tenantId,
            String username
    ) {
    }

    public record RefreshReq(
            @NotBlank(message = "refreshToken不能为空") @Size(min = 16, max = 128, message = "refreshToken长度不合法") String refreshToken
    ) {
    }

    public record LogoutReq(
            @NotBlank(message = "refreshToken不能为空") @Size(min = 16, max = 128, message = "refreshToken长度不合法") String refreshToken
    ) {
    }

    public record TenantSearchReq(
            String keyword,
            Integer limit
    ) {
    }

    public record ResetPasswordReq(
            @NotBlank(message = "phone不能为空") @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
            @NotBlank(message = "smsCode不能为空") @Size(min = 4, max = 8, message = "验证码长度不合法") String smsCode,
            @NotBlank(message = "newPassword不能为空") @Size(min = 8, max = 64, message = "密码长度需在8-64") String newPassword,
            @Size(max = 16, message = "tenantCode长度不能超过16") String tenantCode
    ) {
    }

    public record VerifySmsCodeReq(
            @NotBlank(message = "phone不能为空") @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
            @NotBlank(message = "smsCode不能为空") @Size(min = 4, max = 8, message = "验证码长度不合法") String smsCode,
            @Size(max = 16, message = "tenantCode长度不能超过16") String tenantCode
    ) {
    }

    public record UpdateUserReq(
            @Size(min = 2, max = 30, message = "realName长度需在2-30") String realName,
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
            @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", message = "邮箱格式不正确") String email,
            String avatarUrl
    ) {
    }

    public record ChangePasswordReq(
            @NotBlank(message = "oldPassword不能为空") String oldPassword,
            @NotBlank(message = "newPassword不能为空") @Size(min = 8, max = 20, message = "密码长度需在8-20") String newPassword
    ) {
    }

    public record BindPhoneSendCodeReq(
            @NotBlank(message = "phone不能为空") @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone
    ) {
    }

    public record BindPhoneReq(
            @NotBlank(message = "phone不能为空") @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
            @NotBlank(message = "smsCode不能为空") @Size(min = 4, max = 8, message = "验证码长度不合法") String smsCode
    ) {
    }
}
