package com.plating.erp.auth.vo;

import java.util.List;

public class AuthResponseVo {
    public record LoginUserInfo(Long userId, Long tenantId, String username, String realName, List<String> roles) {
    }

    public record LoginResponse(String accessToken, String refreshToken, Integer expiresIn, LoginUserInfo userInfo) {
    }

    public record CodeSendResponse(String channel, String receiver, boolean sent) {
    }

    public record ScanTicketResponse(String qrToken, Integer expireSeconds) {
    }

    public record ScanConfirmResponse(String qrToken, boolean confirmed) {
    }
}
