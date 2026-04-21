package com.plating.erp.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.iam.entity.RoleEntity;
import com.plating.erp.iam.mapper.RoleMapper;
import com.plating.erp.iam.mapper.UserRoleMapper;
import com.plating.erp.iam.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class RoleServiceImpl implements RoleService {
    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public RoleServiceImpl(RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public PageResult<RoleEntity> page(int pageNum, int pageSize, Integer status, Long tenantId, Boolean isSystem) {
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
        if (!Boolean.TRUE.equals(isSystem)) {
            wrapper.eq(RoleEntity::getTenantId, tenantId);
        }
        if (status != null) {
            wrapper.eq(RoleEntity::getStatus, status);
        }
        wrapper.orderByDesc(RoleEntity::getId);

        Page<RoleEntity> page = roleMapper.selectPage(
                new Page<>(pageNum, pageSize),
                wrapper
        );

        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    @Transactional
    @Override
    public boolean delete(Long roleId, Long expectedTenantId, Boolean isSystem) {
        RoleEntity role = roleMapper.selectById(roleId);
        if (role == null) {
            return false;
        }
        // 注意：selectById 不再自动过滤 tenant_id，必须手动验证
        if (!Boolean.TRUE.equals(isSystem) && !Objects.equals(role.getTenantId(), expectedTenantId)) {
            log.warn("角色不属于当前租户, roleId={}, expectedTenantId={}, actualTenantId={}", 
                    roleId, expectedTenantId, role.getTenantId());
            return false;
        }
        userRoleMapper.deleteByRole(role.getTenantId(), roleId);
        return roleMapper.deleteById(roleId) > 0;
    }

    @Override
    public RoleEntity getById(Long roleId) {
        RoleEntity role = roleMapper.selectById(roleId);
        // 注意：调用方需要确保 roleId 属于正确的租户
        return role;
    }

    @Override
    public boolean save(RoleEntity entity) {
        return roleMapper.insertOrUpdate(entity);
    }

    @Override
    public boolean updateById(RoleEntity entity) {
        return roleMapper.updateById(entity) > 0;
    }
}
