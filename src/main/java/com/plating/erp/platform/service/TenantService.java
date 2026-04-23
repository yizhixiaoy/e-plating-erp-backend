package com.plating.erp.platform.service;

import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.platform.entity.TenantEntity;

import java.util.List;

public interface TenantService {
    PageResult<TenantEntity> page(int pageNum, int pageSize, Integer status, String keyword, Long scope);

    TenantEntity getByShortCode(String shortCode);

    TenantEntity getById(Long tenantId);

    boolean save(TenantEntity entity);

    /**
     * 查询所有租户（下拉选项用）
     */
    List<TenantEntity> listAll();
}
