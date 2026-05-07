package com.plating.erp.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.iam.entity.DeptEntity;
import com.plating.erp.iam.mapper.DeptMapper;
import com.plating.erp.iam.service.DeptService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class DeptServiceImpl implements DeptService {
    private static final Logger log = LoggerFactory.getLogger(DeptServiceImpl.class);

    private final DeptMapper deptMapper;

    public DeptServiceImpl(DeptMapper deptMapper) {
        this.deptMapper = deptMapper;
    }

    @Override
    public PageResult<DeptEntity> page(int pageNum, int pageSize, Long tenantId, Boolean isSystem, Long filterTenantId, String keyword) {
        LambdaQueryWrapper<DeptEntity> wrapper = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(isSystem)) {
            // 平台管理员：可按租户筛选
            if (filterTenantId != null && filterTenantId > 0) {
                wrapper.eq(DeptEntity::getTenantId, filterTenantId);
            }
        } else if (tenantId != null) {
            // 租户用户：只能看自己公司的
            wrapper.eq(DeptEntity::getTenantId, tenantId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(DeptEntity::getDeptName, keyword);
        }
        wrapper.orderByAsc(DeptEntity::getSortNo).orderByDesc(DeptEntity::getId);

        Page<DeptEntity> page = deptMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    @Override
    public List<DeptEntity> listAll(Long tenantId) {
        LambdaQueryWrapper<DeptEntity> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            wrapper.eq(DeptEntity::getTenantId, tenantId);
        }
        wrapper.eq(DeptEntity::getStatus, 0)
               .orderByAsc(DeptEntity::getSortNo)
               .orderByDesc(DeptEntity::getId);
        return deptMapper.selectList(wrapper);
    }

    @Override
    public DeptEntity save(DeptEntity entity) {
        deptMapper.insert(entity);
        return deptMapper.selectById(entity.getId());
    }

    @Override
    public DeptEntity getById(Long deptId) {
        return deptMapper.selectById(deptId);
    }

    @Override
    public boolean updateById(DeptEntity entity) {
        return deptMapper.updateById(entity) > 0;
    }

    @Transactional
    @Override
    public boolean delete(Long deptId, Long tenantId, Boolean isSystem) {
        DeptEntity dept = deptMapper.selectById(deptId);
        if (dept == null) {
            return false;
        }
        if (!Boolean.TRUE.equals(isSystem) && !Objects.equals(dept.getTenantId(), tenantId)) {
            log.warn("部门不属于当前租户, deptId={}, expectedTenantId={}, actualTenantId={}",
                    deptId, tenantId, dept.getTenantId());
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该部门");
        }
        // 检查是否存在子部门
        Long childCount = deptMapper.selectCount(
                new LambdaQueryWrapper<DeptEntity>()
                        .eq(DeptEntity::getParentId, deptId)
                        .eq(DeptEntity::getDeleted, 0)
        );
        if (childCount > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "存在子部门，请先删除子部门");
        }
        return deptMapper.deleteById(deptId) > 0;
    }
}
