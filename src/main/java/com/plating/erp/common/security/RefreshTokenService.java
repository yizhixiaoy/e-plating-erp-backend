package com.plating.erp.common.security;

public interface RefreshTokenService {
    String create(Long userId, Long tenantId);

    String validate(String refreshToken);

    void invalidate(String refreshToken);

    void invalidateAllForUser(long tenantId, long userId);
}
