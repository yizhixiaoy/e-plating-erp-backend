package com.plating.erp.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component("authz")
public class AuthzService {
    private final PermissionMapper permissionMapper;
    private final StringRedisTemplate redisTemplate;
    private final long cacheSeconds;

    public AuthzService(PermissionMapper permissionMapper,
                        StringRedisTemplate redisTemplate,
                        @Value("${app.authz.cache-seconds:120}") long cacheSeconds) {
        this.permissionMapper = permissionMapper;
        this.redisTemplate = redisTemplate;
        this.cacheSeconds = cacheSeconds;
    }

    public boolean hasRole(String roleKey) {
        CurrentUser user = SecurityUtils.currentUser();
        if (user.isSystem()) return true;
        List<String> roles = getRoleKeys(user);
        return roles.contains(roleKey);
    }

    public boolean hasPerm(String perm) {
        CurrentUser user = SecurityUtils.currentUser();
        if (user.isSystem()) return true;
        List<String> perms = getPermKeys(user);
        return perms.contains(perm);
    }

    private List<String> getRoleKeys(CurrentUser user) {
        String key = "erp:authz:roles:" + user.tenantId() + ":" + user.userId();
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null && !cached.isBlank()) {
            return List.of(cached.split(","));
        }
        List<String> roles = permissionMapper.selectRoleKeys(user.userId(), user.tenantId());
        redisTemplate.opsForValue().set(key, String.join(",", roles), cacheSeconds, TimeUnit.SECONDS);
        return roles;
    }

    private List<String> getPermKeys(CurrentUser user) {
        String key = "erp:authz:perms:" + user.tenantId() + ":" + user.userId();
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null && !cached.isBlank()) {
            return List.of(cached.split(","));
        }
        List<String> perms = permissionMapper.selectPerms(user.userId(), user.tenantId());
        redisTemplate.opsForValue().set(key, String.join(",", perms), cacheSeconds, TimeUnit.SECONDS);
        return perms;
    }
}
