package com.plating.erp.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private static final String PREFIX = "erp:refresh:";
    private final StringRedisTemplate redisTemplate;
    private final long refreshExpireSeconds;

    public RefreshTokenService(StringRedisTemplate redisTemplate,
                               @Value("${app.jwt.refresh-expire-seconds}") long refreshExpireSeconds) {
        this.redisTemplate = redisTemplate;
        this.refreshExpireSeconds = refreshExpireSeconds;
    }

    public String create(Long userId, Long tenantId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(PREFIX + token, userId + ":" + tenantId, Duration.ofSeconds(refreshExpireSeconds));
        return token;
    }

    public String validate(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        return redisTemplate.opsForValue().get(PREFIX + refreshToken);
    }

    public void invalidate(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            redisTemplate.delete(PREFIX + refreshToken);
        }
    }
}
