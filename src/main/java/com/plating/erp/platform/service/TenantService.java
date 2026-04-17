package com.plating.erp.platform.service;

import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.platform.entity.TenantEntity;

public interface TenantService {
    PageResult<TenantEntity> page(int pageNum, int pageSize, Integer status, String keyword, Long scope);

    TenantEntity getByShortCode(String shortCode);

    TenantEntity getById(Long tenantId);

    boolean save(TenantEntity entity);
}
