package com.plating.erp.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.platform.entity.TenantEntity;
import com.plating.erp.platform.mapper.TenantMapper;
import com.plating.erp.platform.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TenantServiceImpl implements TenantService {
    private static final Logger log = LoggerFactory.getLogger(TenantServiceImpl.class);

    private final TenantMapper tenantMapper;

    public TenantServiceImpl(TenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    @Override
    public PageResult<TenantEntity> page(int pageNum, int pageSize, Integer status, String keyword, Long scope) {
        LambdaQueryWrapper<TenantEntity> wrapper = new LambdaQueryWrapper<>();
        if (scope != null) {
            wrapper.eq(TenantEntity::getId, scope);
        }
        if (status != null) {
            wrapper.eq(TenantEntity::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(q -> q
                    .like(TenantEntity::getShortCode, keyword)
                    .or()
                    .like(TenantEntity::getTenantName, keyword)
                    .or()
                    .like(TenantEntity::getPhone, keyword)
            );
        }
        wrapper.orderByDesc(TenantEntity::getId);

        Page<TenantEntity> page = tenantMapper.selectPage(
                new Page<>(pageNum, pageSize),
                wrapper
        );

        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    @Override
    public TenantEntity getByShortCode(String shortCode) {
        return tenantMapper.selectOne(
                new LambdaQueryWrapper<TenantEntity>()
                        .eq(TenantEntity::getShortCode, shortCode)
                        .last("limit 1")
        );
    }

    @Override
    public TenantEntity getById(Long tenantId) {
        return tenantMapper.selectById(tenantId);
    }

    @Override
    public boolean save(TenantEntity entity) {
        return tenantMapper.insertOrUpdate(entity);
    }
}
