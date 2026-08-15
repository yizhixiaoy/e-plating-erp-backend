package com.plating.erp.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.biz.entity.CustomerEntity;
import com.plating.erp.biz.mapper.CustomerMapper;
import com.plating.erp.biz.service.CustomerService;
import com.plating.erp.biz.vo.CustomerVo;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 客户公司Service实现
 *
 * @author Plating ERP Team
 */
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    private final CustomerMapper customerMapper;

    @Override
    public PageResult<CustomerEntity> page(Long tenantId, String keyword, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<CustomerEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerEntity::getTenantId, tenantId)
               .eq(CustomerEntity::getDeleted, 0);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(CustomerEntity::getCustomerName, keyword)
                    .or().like(CustomerEntity::getContactPerson, keyword)
                    .or().like(CustomerEntity::getContactPhone, keyword));
        }
        wrapper.orderByDesc(CustomerEntity::getId);

        Page<CustomerEntity> page = customerMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    @Override
    public List<CustomerEntity> listAll(Long tenantId) {
        LambdaQueryWrapper<CustomerEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerEntity::getTenantId, tenantId)
               .eq(CustomerEntity::getDeleted, 0)
               .orderByAsc(CustomerEntity::getId);
        return customerMapper.selectList(wrapper);
    }

    @Override
    public CustomerVo.CustomerDetailVo getById(Long id, Long tenantId) {
        CustomerEntity entity = customerMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "客户公司不存在");
        }
        if (!entity.getTenantId().equals(tenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该客户公司");
        }
        return toDetailVo(entity);
    }

    @Override
    public CustomerEntity create(CustomerVo.CustomerCreateReq req, Long tenantId, Long operatorId) {
        // 检查名称是否重复
        LambdaQueryWrapper<CustomerEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerEntity::getTenantId, tenantId)
               .eq(CustomerEntity::getCustomerName, req.customerName())
               .eq(CustomerEntity::getDeleted, 0);
        Long count = customerMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BizException(ErrorCode.CONFLICT, "客户公司名称已存在");
        }

        CustomerEntity entity = new CustomerEntity();
        entity.setTenantId(tenantId);
        entity.setCustomerName(req.customerName());
        entity.setContactPerson(req.contactPerson());
        entity.setContactPhone(req.contactPhone());
        entity.setAddress(req.address());
        entity.setRemark(req.remark());
        entity.setCreatedBy(operatorId);
        customerMapper.insert(entity);
        log.info("创建客户公司成功: id={}, name={}", entity.getId(), entity.getCustomerName());
        return entity;
    }

    @Override
    public boolean update(Long id, CustomerVo.CustomerUpdateReq req, Long tenantId, Long operatorId) {
        CustomerEntity entity = customerMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "客户公司不存在");
        }
        if (!entity.getTenantId().equals(tenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权修改该客户公司");
        }

        entity.setCustomerName(req.customerName() != null ? req.customerName() : entity.getCustomerName());
        entity.setContactPerson(req.contactPerson() != null ? req.contactPerson() : entity.getContactPerson());
        entity.setContactPhone(req.contactPhone() != null ? req.contactPhone() : entity.getContactPhone());
        entity.setAddress(req.address() != null ? req.address() : entity.getAddress());
        entity.setRemark(req.remark() != null ? req.remark() : entity.getRemark());
        entity.setUpdatedBy(operatorId);
        return customerMapper.updateById(entity) > 0;
    }

    @Override
    public boolean delete(Long id, Long tenantId) {
        CustomerEntity entity = customerMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "客户公司不存在");
        }
        if (!entity.getTenantId().equals(tenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权删除该客户公司");
        }
        return customerMapper.deleteById(id) > 0;
    }

    private CustomerVo.CustomerDetailVo toDetailVo(CustomerEntity entity) {
        return new CustomerVo.CustomerDetailVo(
                entity.getId(),
                entity.getTenantId(),
                entity.getCustomerName(),
                entity.getContactPerson(),
                entity.getContactPhone(),
                entity.getAddress(),
                entity.getRemark(),
                entity.getCreatedBy(),
                null,
                entity.getCreatedAt(),
                entity.getUpdatedBy(),
                null,
                entity.getUpdatedAt()
        );
    }
}
