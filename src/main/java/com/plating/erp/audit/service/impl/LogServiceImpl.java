package com.plating.erp.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.audit.entity.BizLogEntity;
import com.plating.erp.audit.entity.OperLogEntity;
import com.plating.erp.audit.mapper.BizLogMapper;
import com.plating.erp.audit.mapper.OperLogMapper;
import com.plating.erp.audit.service.LogService;
import org.springframework.stereotype.Service;

@Service
public class LogServiceImpl implements LogService {
    private final OperLogMapper operLogMapper;
    private final BizLogMapper bizLogMapper;

    public LogServiceImpl(OperLogMapper operLogMapper, BizLogMapper bizLogMapper) {
        this.operLogMapper = operLogMapper;
        this.bizLogMapper = bizLogMapper;
    }

    @Override
    public Page<OperLogEntity> pageOper(int pageNum, int pageSize, String moduleTitle, Integer status,
                                        Long scopeTenantId, boolean allTenants) {
        LambdaQueryWrapper<OperLogEntity> qw = new LambdaQueryWrapper<>();
        if (!allTenants && scopeTenantId != null) {
            qw.eq(OperLogEntity::getTenantId, scopeTenantId);
        }
        qw.like(moduleTitle != null && !moduleTitle.isBlank(), OperLogEntity::getModuleTitle, moduleTitle)
                .eq(status != null, OperLogEntity::getStatus, status)
                .orderByDesc(OperLogEntity::getId);
        return operLogMapper.selectPage(new Page<>(pageNum, pageSize), qw);
    }

    @Override
    public OperLogEntity getOper(Long id) {
        return operLogMapper.selectById(id);
    }

    @Override
    public Page<BizLogEntity> pageBiz(int pageNum, int pageSize, String bizModule, Long bizId,
                                      Long scopeTenantId, boolean allTenants) {
        LambdaQueryWrapper<BizLogEntity> qw = new LambdaQueryWrapper<>();
        if (!allTenants && scopeTenantId != null) {
            qw.eq(BizLogEntity::getTenantId, scopeTenantId);
        }
        qw.eq(bizModule != null && !bizModule.isBlank(), BizLogEntity::getBizModule, bizModule)
                .eq(bizId != null, BizLogEntity::getBizId, bizId)
                .orderByDesc(BizLogEntity::getId);
        return bizLogMapper.selectPage(new Page<>(pageNum, pageSize), qw);
    }
}
