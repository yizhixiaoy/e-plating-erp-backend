package com.plating.erp.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.iam.entity.PositionEntity;
import com.plating.erp.iam.mapper.PositionMapper;
import com.plating.erp.iam.service.PositionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class PositionServiceImpl implements PositionService {
    private static final Logger log = LoggerFactory.getLogger(PositionServiceImpl.class);

    private final PositionMapper positionMapper;

    public PositionServiceImpl(PositionMapper positionMapper) {
        this.positionMapper = positionMapper;
    }

    @Override
    public PageResult<PositionEntity> page(int pageNum, int pageSize, Long tenantId, Boolean isSystem, Long filterTenantId, Long filterDeptId, String keyword) {
        LambdaQueryWrapper<PositionEntity> wrapper = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(isSystem)) {
            // 平台管理员：可按租户筛选
            if (filterTenantId != null && filterTenantId > 0) {
                wrapper.eq(PositionEntity::getTenantId, filterTenantId);
            }
        } else if (tenantId != null) {
            // 租户用户：只能看自己公司的
            wrapper.eq(PositionEntity::getTenantId, tenantId);
        }
        // 按部门筛选（平台和租户均可）
        if (filterDeptId != null && filterDeptId > 0) {
            wrapper.eq(PositionEntity::getDeptId, filterDeptId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(PositionEntity::getPositionName, keyword);
        }
        wrapper.orderByAsc(PositionEntity::getSortNo).orderByDesc(PositionEntity::getId);
        Page<PositionEntity> page = positionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    @Override
    public List<PositionEntity> listAll(Long tenantId, Long deptId) {
        LambdaQueryWrapper<PositionEntity> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            wrapper.eq(PositionEntity::getTenantId, tenantId);
        }
        if (deptId != null && deptId > 0) {
            wrapper.eq(PositionEntity::getDeptId, deptId);
        }
        wrapper.eq(PositionEntity::getStatus, 0)
               .orderByAsc(PositionEntity::getSortNo)
               .orderByDesc(PositionEntity::getId);
        return positionMapper.selectList(wrapper);
    }

    @Override
    public PositionEntity save(PositionEntity entity) {
        positionMapper.insert(entity);
        return positionMapper.selectById(entity.getId());
    }

    @Override
    public PositionEntity getById(Long positionId) {
        return positionMapper.selectById(positionId);
    }

    @Override
    public boolean updateById(PositionEntity entity) {
        return positionMapper.updateById(entity) > 0;
    }

    @Transactional
    @Override
    public boolean delete(Long positionId, Long tenantId, Boolean isSystem) {
        PositionEntity position = positionMapper.selectById(positionId);
        if (position == null) {
            return false;
        }
        if (!Boolean.TRUE.equals(isSystem) && !Objects.equals(position.getTenantId(), tenantId)) {
            log.warn("岗位不属于当前租户, positionId={}, expectedTenantId={}, actualTenantId={}",
                    positionId, tenantId, position.getTenantId());
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该岗位");
        }
        return positionMapper.deleteById(positionId) > 0;
    }
}
