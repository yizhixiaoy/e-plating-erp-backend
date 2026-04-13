package com.plating.erp.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Marks a time after which JWTs issued earlier must be rejected (e.g. after password reset).
 */
@Service
public class CredentialRevocationService {
    static final String PREFIX = "erp:credential:revoke_at:";

    private final StringRedisTemplate redisTemplate;
    private final long accessTokenExpireSeconds;

    public CredentialRevocationService(StringRedisTemplate redisTemplate,
                                       @Value("${app.jwt.expire-seconds:7200}") long accessTokenExpireSeconds) {
        this.redisTemplate = redisTemplate;
        this.accessTokenExpireSeconds = accessTokenExpireSeconds;
    }

    public void revokeCredentialsIssuedBeforeNow(long tenantId, long userId) {
        String key = PREFIX + tenantId + ":" + userId;
        redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()),
                Duration.ofSeconds(Math.max(accessTokenExpireSeconds, 60)));
    }

    public boolean isIssuedBeforeRevocation(long tenantId, long userId, long tokenIssuedAtMillis) {
        String key = PREFIX + tenantId + ":" + userId;
        String raw = redisTemplate.opsForValue().get(key);
        if (raw == null || raw.isBlank()) {
            return false;
        }
        try {
            long revokeAt = Long.parseLong(raw);
            return tokenIssuedAtMillis < revokeAt;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
