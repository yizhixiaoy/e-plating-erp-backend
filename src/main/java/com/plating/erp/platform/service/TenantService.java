package com.plating.erp.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.platform.entity.TenantEntity;
import com.plating.erp.platform.mapper.TenantMapper;
import org.springframework.stereotype.Service;

@Service
public class TenantService {
    private final TenantMapper tenantMapper;

    public TenantService(TenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    /**
     * @param scopeTenantId 非平台用户时仅允许查看本租户；为 null 表示平台可查全部
     */
    public Page<TenantEntity> page(int pageNum, int pageSize, Integer status, String keyword, Long scopeTenantId) {
        LambdaQueryWrapper<TenantEntity> qw = new LambdaQueryWrapper<>();
        qw.eq(scopeTenantId != null, TenantEntity::getId, scopeTenantId)
                .eq(status != null, TenantEntity::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), w -> w.like(TenantEntity::getTenantName, keyword)
                        .or().like(TenantEntity::getShortCode, keyword))
                .orderByDesc(TenantEntity::getId);
        return tenantMapper.selectPage(new Page<>(pageNum, pageSize), qw);
    }

    public TenantEntity getById(Long id) {
        return tenantMapper.selectById(id);
    }

    public TenantEntity getByShortCode(String shortCode) {
        return tenantMapper.selectOne(new LambdaQueryWrapper<TenantEntity>()
                .eq(TenantEntity::getShortCode, shortCode)
                .last("limit 1"));
    }

    public TenantEntity save(TenantEntity entity) {
        if (entity.getId() == null) {
            entity.setId(IdWorker.getId());
            tenantMapper.insert(entity);
        } else {
            tenantMapper.updateById(entity);
        }
        return entity;
    }
}
