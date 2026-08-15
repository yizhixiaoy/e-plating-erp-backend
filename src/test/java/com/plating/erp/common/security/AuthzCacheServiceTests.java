package com.plating.erp.common.security;

import com.plating.erp.common.security.impl.AuthzCacheServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthzCacheServiceTests {
    @Mock
    private StringRedisTemplate redisTemplate;

    private AuthzCacheService authzCacheService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authzCacheService = new AuthzCacheServiceImpl(redisTemplate);
    }

    @Test
    void evictUser_shouldDeleteRolesAndPermsKeys() {
        authzCacheService.evictUser(20001L, 10001L);
        verify(redisTemplate).delete("erp:authz:roles:20001:10001");
        verify(redisTemplate).delete("erp:authz:perms:20001:10001");
    }

    @Test
    void evictTenant_shouldDeleteMatchedKeys() {
        when(redisTemplate.keys("erp:authz:roles:20001:*"))
                .thenReturn(Set.of("erp:authz:roles:20001:10001"));
        when(redisTemplate.keys("erp:authz:perms:20001:*"))
                .thenReturn(Set.of("erp:authz:perms:20001:10001", "erp:authz:perms:20001:10002"));

        authzCacheService.evictTenant(20001L);

        verify(redisTemplate).delete(Set.of("erp:authz:roles:20001:10001"));
        verify(redisTemplate).delete(Set.of("erp:authz:perms:20001:10001", "erp:authz:perms:20001:10002"));
    }

    @Test
    void evictTenant_whenTenantNull_shouldIgnore() {
        authzCacheService.evictTenant(null);
        verify(redisTemplate, never()).keys(org.mockito.ArgumentMatchers.anyString());
    }
}
