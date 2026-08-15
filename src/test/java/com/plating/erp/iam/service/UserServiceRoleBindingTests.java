package com.plating.erp.iam.service;

import com.plating.erp.common.security.AuthzCacheService;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.entity.UserRoleEntity;
import com.plating.erp.iam.mapper.UserMapper;
import com.plating.erp.iam.mapper.UserRoleMapper;
import com.plating.erp.iam.service.impl.UserServiceImpl;
import com.plating.erp.platform.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceRoleBindingTests {
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private AuthzCacheService authzCacheService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TenantService tenantService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserServiceImpl(userMapper, userRoleMapper, authzCacheService, passwordEncoder,tenantService);
    }

    @Test
    void bindRoles_shouldEvictUserCache() {
        when(userRoleMapper.insert(any(UserRoleEntity.class))).thenReturn(1);
        int count = userService.bindRoles(20001L, 10001L, List.of(40001L, 40002L));
        assertEquals(2, count);
        verify(authzCacheService).evictUser(20001L, 10001L);
    }

    @Test
    void unbindRole_shouldEvictUserCache() {
        when(userRoleMapper.delete(any())).thenReturn(1);
        userService.unbindRole(20001L, 10001L, 40001L);
        verify(authzCacheService).evictUser(20001L, 10001L);
    }

    @Test
    void bindRoles_resolveTenantFromUserWhenMissing() {
        UserEntity user = new UserEntity();
        user.setTenantId(20001L);
        when(userMapper.selectById(10001L)).thenReturn(user);
        when(userRoleMapper.insert(any(UserRoleEntity.class))).thenReturn(1);
        userService.bindRoles(null, 10001L, List.of(40001L));
        verify(authzCacheService, atLeastOnce()).evictUser(20001L, 10001L);
    }
}
