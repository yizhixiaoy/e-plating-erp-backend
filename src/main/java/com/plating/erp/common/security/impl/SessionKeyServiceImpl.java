package com.plating.erp.common.security.impl;

import com.plating.erp.common.security.SessionKeyService;
import com.plating.erp.common.util.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class SessionKeyServiceImpl implements SessionKeyService {

    private static final Logger log = LoggerFactory.getLogger(SessionKeyServiceImpl.class);
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
    public String getOrCreate(Long userId, Long tenantId) {
        String k = key(userId, tenantId);
        // 检查是否已有 key，有则直接复用
        String existing = get(userId, tenantId);
        if (existing != null) {
            // 确保 key 持久化（移除可能被旧逻辑设置的 TTL）
            redisTemplate.persist(k);
            log.debug("[sessionKey] 复用已有 key, userId={} tenantId={}", userId, tenantId);
            return existing;
        }
        // 无已有 key → 用 SETNX 原子创建，不设 TTL（持久化存储，仅登出时删除）
        String sessionKey = CryptoUtil.generateSessionKey();
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(k, sessionKey);
        if (Boolean.TRUE.equals(success)) {
            log.debug("[sessionKey] 新建持久化 key, userId={} tenantId={}", userId, tenantId);
            return sessionKey;
        }
        // SETNX 失败说明已被其他并发请求抢先创建，取已有 key 返回
        existing = get(userId, tenantId);
        if (existing != null) {
            log.debug("[sessionKey] SETNX冲突，取已有 key, userId={} tenantId={}", userId, tenantId);
            return existing;
        }
        // 极端兜底：SETNX 失败但 get 也拿不到（几乎不可能）
        log.warn("[sessionKey] getOrCreate race condition fallback for userId={} tenantId={}", userId, tenantId);
        redisTemplate.opsForValue().set(k, sessionKey);
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

    @Override
    public void touch(Long userId, Long tenantId) {
        // Session key 为持久化存储，无需刷新 TTL。
        // 调用 persist 移除旧版本可能残留的 TTL，确保迁移平滑。
        redisTemplate.persist(key(userId, tenantId));
    }
}
