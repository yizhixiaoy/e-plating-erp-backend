package com.plating.erp.iam.service;

import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.iam.entity.RoleEntity;

import java.util.List;

public interface RoleService {
    PageResult<RoleEntity> page(int pageNum, int pageSize, Integer status, Long tenantId, Boolean isSystem);

    PageResult<RoleEntity> page(int pageNum, int pageSize, Integer status, Long tenantId, Boolean isSystem,
                                Long filterTenantId, Long filterDeptId, String keyword);

    List<RoleEntity> listByTenantAndDept(Long tenantId, Boolean isSystem, Long deptId);

    boolean delete(Long roleId, Long tenantId, Boolean isSystem);

    RoleEntity getById(Long roleId);

    boolean save(RoleEntity entity);

    boolean updateById(RoleEntity entity);
}
