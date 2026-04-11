package com.plating.erp.common.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthzCacheService {
    private final StringRedisTemplate redisTemplate;

    public AuthzCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void evictUser(Long tenantId, Long userId) {
        if (tenantId == null || userId == null) {
            return;
        }
        redisTemplate.delete("erp:authz:roles:" + tenantId + ":" + userId);
        redisTemplate.delete("erp:authz:perms:" + tenantId + ":" + userId);
    }

    public void evictTenant(Long tenantId) {
        if (tenantId == null) {
            return;
        }
        deleteByPattern("erp:authz:roles:" + tenantId + ":*");
        deleteByPattern("erp:authz:perms:" + tenantId + ":*");
    }

    private void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return;
        }
        redisTemplate.delete(keys);
    }
}
