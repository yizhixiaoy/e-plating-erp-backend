package com.plating.erp.auth.vo;

import java.time.LocalDateTime;
import java.util.List;

public class AuthResponseVo {
    public record LoginUserInfo(
            Long userId,
            Long tenantId,
            String username,
            String realName,
            List<String> roles,
            String entryType
    ) {
    }

    public record LoginResponse(
            String accessToken,
            String refreshToken,
            Integer expiresIn,
            LoginUserInfo userInfo
    ) {
    }

    public record CodeSendResponse(String channel, String receiver, boolean sent) {
    }

    public record ScanTicketResponse(String qrToken, String qrUrl, Integer expiresIn) {
    }

    public record ScanConfirmResponse(String qrToken, boolean confirmed) {
    }

    public record ScanStatusResponse(
            String status,
            String accessToken,
            String refreshToken,
            Integer expiresIn,
            LoginUserInfo userInfo
    ) {
    }

    public record TenantSearchResult(
            String shortCode,
            String tenantName,
            String logoUrl
    ) {
    }

    public record RecentTenantResult(
            String shortCode,
            String tenantName,
            String logoUrl,
            LocalDateTime lastLoginTime
    ) {
    }

    public record LoginHistoryResult(
            Long id,
            String loginType,
            LocalDateTime loginTime,
            String loginIp,
            String deviceInfo,
            Integer loginStatus
    ) {
    }

    public record TenantByUsernameResult(
            String shortCode,
            String tenantName,
            String logoUrl,
            Long tenantId,
            String realName,
            String phone,
            String email,
            boolean found
    ) {
    }
}
