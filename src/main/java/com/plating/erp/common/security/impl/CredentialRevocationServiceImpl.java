package com.plating.erp.common.security.impl;

import com.plating.erp.common.security.CredentialRevocationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CredentialRevocationServiceImpl implements CredentialRevocationService {
    static final String PREFIX = "erp:credential:revoke_at:";

    private final StringRedisTemplate redisTemplate;
    private final long accessTokenExpireSeconds;

    public CredentialRevocationServiceImpl(StringRedisTemplate redisTemplate,
                                           @Value("${app.jwt.expire-seconds:7200}") long accessTokenExpireSeconds) {
        this.redisTemplate = redisTemplate;
        this.accessTokenExpireSeconds = accessTokenExpireSeconds;
    }

    @Override
    public void revokeCredentialsIssuedBeforeNow(long tenantId, long userId) {
        String key = PREFIX + tenantId + ":" + userId;
        redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()),
                Duration.ofSeconds(Math.max(accessTokenExpireSeconds, 60)));
    }

    /**
     * 检查Token是否在凭证吊销时间之前签发
     *
     * 用于判断Token是否因密码重置、账号禁用等操作而失效。
     * 如果Token签发时间早于吊销时间，则该Token已失效。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param tokenIssuedAtMillis Token签发时间戳（毫秒）
     * @return true表示Token在吊销前签发（已失效），false表示无吊销记录或Token仍有效
     */
    @Override
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
