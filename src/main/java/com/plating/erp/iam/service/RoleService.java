package com.plating.erp.iam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.iam.entity.RoleEntity;
import com.plating.erp.iam.mapper.RoleMapper;
import com.plating.erp.iam.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class RoleService {
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public RoleService(RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public Page<RoleEntity> page(int pageNum, int pageSize, Integer status, Long scopeTenantId, boolean allTenants) {
        LambdaQueryWrapper<RoleEntity> qw = new LambdaQueryWrapper<>();
        if (!allTenants && scopeTenantId != null) {
            qw.eq(RoleEntity::getTenantId, scopeTenantId);
        }
        qw.eq(status != null, RoleEntity::getStatus, status).orderByDesc(RoleEntity::getId);
        return roleMapper.selectPage(new Page<>(pageNum, pageSize), qw);
    }

    public RoleEntity getById(Long id) {
        return roleMapper.selectById(id);
    }

    public RoleEntity save(RoleEntity entity) {
        if (entity.getId() == null) {
            entity.setId(IdWorker.getId());
            roleMapper.insert(entity);
        } else {
            roleMapper.updateById(entity);
        }
        return entity;
    }

    @Transactional
    public boolean delete(Long roleId, Long expectedTenantId, boolean systemUser) {
        RoleEntity role = roleMapper.selectById(roleId);
        if (role == null) {
            return false;
        }
        if (!systemUser && !Objects.equals(role.getTenantId(), expectedTenantId)) {
            return false;
        }
        userRoleMapper.deleteByRole(role.getTenantId(), roleId);
        return roleMapper.deleteById(roleId) > 0;
    }
}
