package com.plating.erp.iam.service;

import com.plating.erp.iam.entity.RoleEntity;
import com.plating.erp.iam.mapper.RoleMapper;
import com.plating.erp.iam.mapper.UserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleServiceCleanupTests {
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private UserRoleMapper userRoleMapper;

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        roleService = new RoleService(roleMapper, userRoleMapper);
    }

    @Test
    void delete_shouldCleanupUserRoleBeforeDeleteRole() {
        RoleEntity role = new RoleEntity();
        role.setId(40001L);
        role.setTenantId(20001L);
        when(roleMapper.selectById(40001L)).thenReturn(role);
        when(roleMapper.deleteById(40001L)).thenReturn(1);

        boolean deleted = roleService.delete(40001L, 20001L, false);

        assertTrue(deleted);
        verify(userRoleMapper).deleteByRole(20001L, 40001L);
        verify(roleMapper).deleteById(40001L);
    }

    @Test
    void delete_whenRoleMissing_shouldReturnFalse() {
        when(roleMapper.selectById(40001L)).thenReturn(null);

        boolean deleted = roleService.delete(40001L, 20001L, false);

        assertFalse(deleted);
        verify(userRoleMapper, never()).deleteByRole(anyLong(), anyLong());
    }
}
