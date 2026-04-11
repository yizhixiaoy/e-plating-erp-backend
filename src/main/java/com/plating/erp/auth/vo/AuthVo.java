package com.plating.erp.auth.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthVo {
    public record LoginReq(
            @NotBlank(message = "loginType不能为空") String loginType,
            @NotBlank(message = "tenantCode不能为空") @Size(max = 16, message = "tenantCode长度不能超过16") @Pattern(regexp = "^[a-z]+$", message = "tenantCode仅支持小写字母") String tenantCode,
            @NotBlank(message = "username不能为空") @Size(min = 2, max = 32, message = "username长度需在2-32") @Pattern(regexp = "^[a-zA-Z0-9:_-]+$", message = "username格式不合法") String username,
            @NotBlank(message = "password不能为空") @Size(min = 6, max = 64, message = "password长度需在6-64") String password,
            @NotBlank(message = "clientType不能为空") String clientType
    ) {
    }

    public record SmsCodeReq(@NotBlank(message = "phone不能为空") @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone) {
    }

    public record EmailCodeReq(@NotBlank(message = "email不能为空") @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", message = "邮箱格式不正确") String email) {
    }

    public record ScanConfirmReq(@NotBlank(message = "qrToken不能为空") @Size(max = 128, message = "qrToken长度不能超过128") String qrToken) {
    }

    public record RefreshReq(@NotBlank(message = "refreshToken不能为空") @Size(min = 16, max = 128, message = "refreshToken长度不合法") String refreshToken) {
    }

    public record LogoutReq(@NotBlank(message = "refreshToken不能为空") @Size(min = 16, max = 128, message = "refreshToken长度不合法") String refreshToken) {
    }
}
