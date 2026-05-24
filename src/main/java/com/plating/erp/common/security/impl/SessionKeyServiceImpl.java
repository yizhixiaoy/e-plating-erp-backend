package com.plating.erp.common.security.impl;

import com.plating.erp.common.security.SessionKeyService;
import com.plating.erp.common.util.CryptoUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class SessionKeyServiceImpl implements SessionKeyService {

    private static final String PREFIX = "erp:sessionkey:";

    private final StringRedisTemplate redisTemplate;
    private final long expireSeconds;

    public SessionKeyServiceImpl(StringRedisTemplate redisTemplate,
                                 @Value("${app.jwt.expire-seconds:7200}") long expireSeconds) {
        this.redisTemplate = redisTemplate;
        this.expireSeconds = expireSeconds;
    }

    private static String key(Long userId, Long tenantId) {
        return PREFIX + tenantId + ":" + userId;
    }

    @Override
    public String create(Long userId, Long tenantId) {
        String sessionKey = CryptoUtil.generateSessionKey();
        redisTemplate.opsForValue().set(key(userId, tenantId), sessionKey, Duration.ofSeconds(expireSeconds));
        return sessionKey;
    }

    @Override
    public String get(Long userId, Long tenantId) {
        return redisTemplate.opsForValue().get(key(userId, tenantId));
    }

    @Override
    public void remove(Long userId, Long tenantId) {
        redisTemplate.delete(key(userId, tenantId));
    }
}
