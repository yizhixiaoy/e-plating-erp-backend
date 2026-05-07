package com.plating.erp.auth.vo;

import java.time.LocalDateTime;
import java.util.List;

public class AuthResponseVo {
    public record LoginUserInfo(
            Long userId,
            Long tenantId,
            String username,
            String realName,
            String avatarUrl,
            List<String> roles,
            String entryType,
            Integer userType,
            // 公司信息
            String companyName,
            String companyLogoUrl,
            // 欢迎语配置
            String welcomeText
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

    public record ScanTicketResponse(String qrToken, String qrImage, Integer expiresIn) {
    }

    public record ScanResponse(String qrToken, boolean scanned) {
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

    public record TenantByPhoneResult(
            String shortCode,
            String tenantName,
            Long tenantId,
            boolean found
    ) {
    }

    public record UserInfoResult(
            Long id,
            String username,
            String realName,
            String phone,
            String email,
            String avatarUrl,
            Long tenantId,
            Integer userType,
            // 权限信息（菜单权限标识）
            List<String> permissions,
            // 公司信息（租户信息）
            String companyName,
            String companyShortCode,
            String companyContact,
            String companyPhone,
            String companyLogoUrl,
            // 部门信息
            String deptName,
            // 岗位信息
            String position,
            // 直属领导信息
            Long leaderUserId,
            String leaderName,
            // 最后登录时间
            String lastLoginAt
    ) {
    }
}
