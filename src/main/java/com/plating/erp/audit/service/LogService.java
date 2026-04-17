package com.plating.erp.audit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.audit.entity.BizLogEntity;
import com.plating.erp.audit.entity.OperLogEntity;

public interface LogService {
    Page<OperLogEntity> pageOper(int pageNum, int pageSize, String moduleTitle, Integer status,
                                 Long scopeTenantId, boolean allTenants);

    OperLogEntity getOper(Long id);

    Page<BizLogEntity> pageBiz(int pageNum, int pageSize, String bizModule, Long bizId,
                               Long scopeTenantId, boolean allTenants);
}
