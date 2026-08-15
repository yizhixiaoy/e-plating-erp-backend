package com.plating.erp.iam.service;

import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.iam.entity.PositionEntity;

import java.util.List;

public interface PositionService {

    /**
     * 分页查询岗位列表（带租户隔离）
     * @param filterTenantId 平台管理员可按租户筛选，非平台用户忽略
     * @param filterDeptId 平台或租户管理员可按部门筛选
     */
    PageResult<PositionEntity> page(int pageNum, int pageSize, Long tenantId, Boolean isSystem, Long filterTenantId, Long filterDeptId, String keyword);

    List<PositionEntity> listAll(Long tenantId, Long deptId);

    PositionEntity save(PositionEntity entity);

    PositionEntity getById(Long positionId);

    boolean updateById(PositionEntity entity);

    boolean delete(Long positionId, Long tenantId, Boolean isSystem);
}
