package com.plating.erp.iam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.iam.entity.RoleEntity;
import com.plating.erp.iam.mapper.RoleMapper;
import com.plating.erp.iam.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public RoleService(RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public Page<RoleEntity> page(int pageNum, int pageSize, Integer status) {
        LambdaQueryWrapper<RoleEntity> qw = new LambdaQueryWrapper<>();
        qw.eq(status != null, RoleEntity::getStatus, status).orderByDesc(RoleEntity::getId);
        return roleMapper.selectPage(new Page<>(pageNum, pageSize), qw);
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
    public boolean delete(Long roleId) {
        RoleEntity role = roleMapper.selectById(roleId);
        if (role == null) {
            return false;
        }
        userRoleMapper.deleteByRole(role.getTenantId(), roleId);
        return roleMapper.deleteById(roleId) > 0;
    }
}
