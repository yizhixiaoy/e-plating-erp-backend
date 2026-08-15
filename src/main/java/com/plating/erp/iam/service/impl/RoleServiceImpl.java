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

import java.util.List;
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
        return page(pageNum, pageSize, status, tenantId, isSystem, null, null, null);
    }

    @Override
    public PageResult<RoleEntity> page(int pageNum, int pageSize, Integer status, Long tenantId, Boolean isSystem,
                                       Long filterTenantId, Long filterDeptId, String keyword) {
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
        // 平台管理员可按指定租户过滤，非平台管理员只能看自己公司的
        if (Boolean.TRUE.equals(isSystem)) {
            if (filterTenantId != null) {
                wrapper.eq(RoleEntity::getTenantId, filterTenantId);
            }
        } else {
            wrapper.eq(RoleEntity::getTenantId, tenantId);
        }
        if (status != null) {
            wrapper.eq(RoleEntity::getStatus, status);
        }
        if (filterDeptId != null) {
            wrapper.eq(RoleEntity::getDeptId, filterDeptId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(RoleEntity::getRoleName, keyword)
                    .or()
                    .like(RoleEntity::getRoleKey, keyword)
            );
        }
        wrapper.orderByDesc(RoleEntity::getId);

        Page<RoleEntity> page = roleMapper.selectPage(
                new Page<>(pageNum, pageSize),
                wrapper
        );

        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    @Override
    public List<RoleEntity> listByTenantAndDept(Long tenantId, Boolean isSystem, Long deptId) {
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
        if (!Boolean.TRUE.equals(isSystem) && tenantId != null) {
            wrapper.eq(RoleEntity::getTenantId, tenantId);
        }
        if (deptId != null && deptId > 0) {
            wrapper.eq(RoleEntity::getDeptId, deptId);
        }
        wrapper.eq(RoleEntity::getStatus, 0)
               .orderByDesc(RoleEntity::getId);
        return roleMapper.selectList(wrapper);
    }

    @Transactional
    @Override
    public boolean delete(Long roleId, Long expectedTenantId, Boolean isSystem) {
        RoleEntity role = roleMapper.selectById(roleId);
        if (role == null) {
            return false;
        }
        if (!Boolean.TRUE.equals(isSystem) && !Objects.equals(role.getTenantId(), expectedTenantId)) {
            log.warn("角色不属于当前租户, roleId={}, expectedTenantId={}, actualTenantId={}",
                    roleId, expectedTenantId, role.getTenantId());
            return false;
        }
        // 删除角色与用户的关联
        userRoleMapper.deleteByRole(role.getTenantId(), roleId);
        // 删除角色本身
        roleMapper.deleteById(roleId);
        return true;
    }

    @Override
    public RoleEntity getById(Long roleId) {
        return roleMapper.selectById(roleId);
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
