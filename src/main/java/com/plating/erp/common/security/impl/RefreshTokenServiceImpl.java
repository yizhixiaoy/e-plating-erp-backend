package com.plating.erp.common.security.impl;

import com.plating.erp.common.security.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private static final String PREFIX = "erp:refresh:";
    private static final String INDEX_PREFIX = "erp:refresh:index:";
    private final StringRedisTemplate redisTemplate;
    private final long refreshExpireSeconds;

    public RefreshTokenServiceImpl(StringRedisTemplate redisTemplate,
                                   @Value("${app.jwt.refresh-expire-seconds}") long refreshExpireSeconds) {
        this.redisTemplate = redisTemplate;
        this.refreshExpireSeconds = refreshExpireSeconds;
    }

    private static String indexKey(long tenantId, long userId) {
        return INDEX_PREFIX + tenantId + ":" + userId;
    }

    @Override
    public String create(Long userId, Long tenantId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(PREFIX + token, userId + ":" + tenantId, Duration.ofSeconds(refreshExpireSeconds));
        String ik = indexKey(tenantId, userId);
        redisTemplate.opsForSet().add(ik, token);
        redisTemplate.expire(ik, Duration.ofSeconds(refreshExpireSeconds));
        return token;
    }

    @Override
    public String validate(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        return redisTemplate.opsForValue().get(PREFIX + refreshToken);
    }

    @Override
    public void invalidate(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        String payload = redisTemplate.opsForValue().get(PREFIX + refreshToken);
        redisTemplate.delete(PREFIX + refreshToken);
        if (payload != null) {
            String[] parts = payload.split(":");
            if (parts.length == 2) {
                try {
                    long userId = Long.parseLong(parts[0]);
                    long tenantId = Long.parseLong(parts[1]);
                    redisTemplate.opsForSet().remove(indexKey(tenantId, userId), refreshToken);
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    @Override
    public void invalidateAllForUser(long tenantId, long userId) {
        String ik = indexKey(tenantId, userId);
        var tokens = redisTemplate.opsForSet().members(ik);
        if (tokens != null) {
            for (String t : tokens) {
                if (t != null && !t.isBlank()) {
                    redisTemplate.delete(PREFIX + t);
                }
            }
        }
        redisTemplate.delete(ik);
    }
}
