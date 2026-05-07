package com.plating.erp.iam.service;

import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.iam.entity.DeptEntity;

import java.util.List;

public interface DeptService {

    /**
     * 分页查询部门列表（带租户隔离）
     * @param filterTenantId 平台管理员可按租户筛选，非平台用户忽略
     */
    PageResult<DeptEntity> page(int pageNum, int pageSize, Long tenantId, Boolean isSystem, Long filterTenantId, String keyword);

    /**
     * 查询所有部门（用于树形展示/下拉选项）
     */
    List<DeptEntity> listAll(Long tenantId);

    /**
     * 新增部门
     */
    DeptEntity save(DeptEntity entity);

    /**
     * 根据ID查询部门
     */
    DeptEntity getById(Long deptId);

    /**
     * 更新部门
     */
    boolean updateById(DeptEntity entity);

    /**
     * 删除部门（逻辑删除，检查子部门）
     */
    boolean delete(Long deptId, Long tenantId, Boolean isSystem);
}
