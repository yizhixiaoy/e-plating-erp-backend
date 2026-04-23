package com.plating.erp.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.AuthzCacheService;
import com.plating.erp.common.util.FileUploadUtils;
import com.plating.erp.common.vo.FileUploadVO;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.entity.UserRoleEntity;
import com.plating.erp.iam.mapper.UserMapper;
import com.plating.erp.iam.mapper.UserRoleMapper;
import com.plating.erp.iam.service.UserService;
import com.plating.erp.platform.entity.TenantEntity;
import com.plating.erp.platform.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final AuthzCacheService authzCacheService;
    private final PasswordEncoder passwordEncoder;
    private final TenantService tenantService;

    public UserServiceImpl(UserMapper userMapper, UserRoleMapper userRoleMapper,
                           AuthzCacheService authzCacheService, PasswordEncoder passwordEncoder,
                           TenantService tenantService) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.authzCacheService = authzCacheService;
        this.passwordEncoder = passwordEncoder;
        this.tenantService = tenantService;
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
                                       String keyword, Long tenantId) {
        IPage<UserEntity> page = userMapper.selectPageWithTenantName(
                new Page<>(pageNum, pageSize),
                tenantId,
                deptId,
                status,
                keyword
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
        // 注意：调用方需要确保 userId 属于正确的租户
        // 如果是租户用户查询，应该在 Controller 层传入 tenantId 并验证
        return userMapper.selectById(userId);
    }

    @Override
    public boolean save(UserEntity entity) {
        if (entity.getPasswordHash() != null && !entity.getPasswordHash().startsWith("$2a$")) {
            entity.setPasswordHash(passwordEncoder.encode(entity.getPasswordHash()));
        }
        return userMapper.insertOrUpdate(entity);
    }

    @Override
    public String updateAvatar(Long userId, MultipartFile avatarFile) {
        try {
            // 头像存储路径: iam/avatar/{yyyy}_{MM}_{dd}/{uuid}.{ext}
            FileUploadVO uploadResult = FileUploadUtils.upload(avatarFile, "iam/avatar");
            
            UserEntity user = userMapper.selectById(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            // 注意：应该验证用户是否属于当前租户（由 Controller 层保证）
            user.setAvatarUrl(uploadResult.getOssPath());
            userMapper.updateById(user);
            
            log.info("用户头像更新成功: userId={}, ossPath={}", userId, uploadResult.getOssPath());
            return uploadResult.getOssPath();
        } catch (Exception e) {
            log.error("用户头像更新失败: userId={}", userId, e);
            throw new RuntimeException("头像上传失败: " + e.getMessage());
        }
    }

    private Long resolveTenantId(Long tenantId, Long userId) {
        if (tenantId != null) {
            return tenantId;
        }
        UserEntity user = userMapper.selectById(userId);
        // 注意：这里获取用户的 tenant_id 用于后续操作
        return user == null ? null : user.getTenantId();
    }

    @Override
    public String generateUsername(Long tenantId) {
        if (tenantId == null) {
            return "USR-0001";
        }
        TenantEntity tenant = tenantService.getById(tenantId);
        String prefix = (tenant != null && tenant.getShortCode() != null && !tenant.getShortCode().isEmpty())
                ? tenant.getShortCode().toUpperCase()
                : "USR";

        // 查询该租户下已有的最大同前缀账号
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getTenantId, tenantId)
                .likeRight(UserEntity::getUsername, prefix + "-")
                .orderByDesc(UserEntity::getUsername)
                .last("LIMIT 1");
        UserEntity lastUser = userMapper.selectOne(wrapper);

        int nextSeq = 1;
        if (lastUser != null && lastUser.getUsername() != null) {
            try {
                String seqPart = lastUser.getUsername().substring(prefix.length() + 1);
                nextSeq = Integer.parseInt(seqPart) + 1;
            } catch (NumberFormatException e) {
                // 如果解析失败，从1开始
                nextSeq = 1;
            }
        }

        return String.format("%s-%04d", prefix, nextSeq);
    }
}
