package com.plating.erp.iam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.common.security.AuthzCacheService;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.entity.UserRoleEntity;
import com.plating.erp.iam.mapper.UserRoleMapper;
import com.plating.erp.iam.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final AuthzCacheService authzCacheService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper,
                       UserRoleMapper userRoleMapper,
                       AuthzCacheService authzCacheService,
                       PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.authzCacheService = authzCacheService;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<UserEntity> page(int pageNum, int pageSize, Long deptId, Integer status) {
        LambdaQueryWrapper<UserEntity> qw = new LambdaQueryWrapper<>();
        qw.eq(status != null, UserEntity::getStatus, status)
                .eq(deptId != null, UserEntity::getDeptId, deptId)
                .orderByDesc(UserEntity::getId);
        return userMapper.selectPage(new Page<>(pageNum, pageSize), qw);
    }

    public UserEntity getById(Long id) {
        return userMapper.selectById(id);
    }

    public UserEntity save(UserEntity entity) {
        if (entity.getPasswordHash() != null && !entity.getPasswordHash().startsWith("$2a$")) {
            entity.setPasswordHash(passwordEncoder.encode(entity.getPasswordHash()));
        }
        if (entity.getId() == null) {
            entity.setId(IdWorker.getId());
            userMapper.insert(entity);
        } else {
            userMapper.updateById(entity);
        }
        return entity;
    }

    public UserEntity findByUsernameAndTenantId(String username, Long tenantId) {
        return userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username)
                .eq(UserEntity::getTenantId, tenantId)
                .last("limit 1"));
    }

    @Transactional
    public int bindRoles(Long tenantId, Long userId, List<Long> roleIds) {
        Long resolvedTenantId = resolveTenantId(tenantId, userId);
        if (resolvedTenantId == null) {
            return 0;
        }
        userRoleMapper.deleteByUser(resolvedTenantId, userId);
        int count = 0;
        for (Long roleId : roleIds) {
            UserRoleEntity rel = new UserRoleEntity();
            rel.setId(IdWorker.getId());
            rel.setTenantId(resolvedTenantId);
            rel.setUserId(userId);
            rel.setRoleId(roleId);
            count += userRoleMapper.insert(rel);
        }
        authzCacheService.evictUser(resolvedTenantId, userId);
        return count;
    }

    @Transactional
    public boolean unbindRole(Long tenantId, Long userId, Long roleId) {
        Long resolvedTenantId = resolveTenantId(tenantId, userId);
        if (resolvedTenantId == null) {
            return false;
        }
        int deleted = userRoleMapper.delete(new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getTenantId, resolvedTenantId)
                .eq(UserRoleEntity::getUserId, userId)
                .eq(UserRoleEntity::getRoleId, roleId));
        authzCacheService.evictUser(resolvedTenantId, userId);
        return deleted > 0;
    }

    private Long resolveTenantId(Long tenantId, Long userId) {
        if (tenantId != null) {
            return tenantId;
        }
        UserEntity user = userMapper.selectById(userId);
        return user == null ? null : user.getTenantId();
    }
}
