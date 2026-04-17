package com.plating.erp.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.AuthzCacheService;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.entity.UserRoleEntity;
import com.plating.erp.iam.mapper.UserMapper;
import com.plating.erp.iam.mapper.UserRoleMapper;
import com.plating.erp.iam.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final AuthzCacheService authzCacheService;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, UserRoleMapper userRoleMapper,
                           AuthzCacheService authzCacheService, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.authzCacheService = authzCacheService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserEntity findByUsernameAndTenantId(String username, Long tenantId) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUsername, username)
                        .eq(UserEntity::getTenantId, tenantId)
                        .last("limit 1")
        );
    }

    @Override
    public UserEntity findByUsername(String username, Long tenantId) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUsername, username)
                        .eq(UserEntity::getTenantId, tenantId)
        );
    }

    @Override
    public UserEntity findByPhone(String phone, Long tenantId) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getPhone, phone)
                        .eq(UserEntity::getTenantId, tenantId)
        );
    }

    @Override
    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Override
    public boolean updatePassword(Long userId, String newPassword) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        return true;
    }

    @Override
    public PageResult<UserEntity> page(int pageNum, int pageSize, Long deptId, Integer status, 
                                       String keyword, Long tenantId, Boolean isSystem) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        
        // 租户隔离：非平台用户只能查看本租户数据
        if (!Boolean.TRUE.equals(isSystem)) {
            wrapper.eq(UserEntity::getTenantId, tenantId);
        } else if (tenantId != null) {
            // 平台管理员可以指定租户ID查询
            wrapper.eq(UserEntity::getTenantId, tenantId);
        }
        
        // 部门筛选
        if (deptId != null) {
            wrapper.eq(UserEntity::getDeptId, deptId);
        }
        
        // 状态筛选
        if (status != null) {
            wrapper.eq(UserEntity::getStatus, status);
        }
        
        // 关键字模糊查询（姓名、账号、手机号）
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like(UserEntity::getRealName, keyword)
                    .or()
                    .like(UserEntity::getUsername, keyword)
                    .or()
                    .like(UserEntity::getPhone, keyword)
            );
        }
        
        // 按更新时间降序排序
        wrapper.orderByDesc(UserEntity::getUpdatedAt);

        Page<UserEntity> page = userMapper.selectPage(
                new Page<>(pageNum, pageSize),
                wrapper
        );

        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    @Transactional
    @Override
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
    @Override
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

    @Override
    public UserEntity getById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public boolean save(UserEntity entity) {
        if (entity.getPasswordHash() != null && !entity.getPasswordHash().startsWith("$2a$")) {
            entity.setPasswordHash(passwordEncoder.encode(entity.getPasswordHash()));
        }
        if (entity.getId() == null) {
            entity.setId(IdWorker.getId());
        }
        return userMapper.insertOrUpdate(entity);
    }

    private Long resolveTenantId(Long tenantId, Long userId) {
        if (tenantId != null) {
            return tenantId;
        }
        UserEntity user = userMapper.selectById(userId);
        return user == null ? null : user.getTenantId();
    }
}
