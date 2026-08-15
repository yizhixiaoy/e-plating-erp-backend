package com.plating.erp.biz.service;

import com.plating.erp.biz.entity.CustomerEntity;
import com.plating.erp.biz.vo.CustomerVo;
import com.plating.erp.common.api.response.PageResult;

import java.util.List;

/**
 * 客户公司Service
 *
 * @author Plating ERP Team
 */
public interface CustomerService {

    /**
     * 分页查询客户公司列表
     */
    PageResult<CustomerEntity> page(Long tenantId, String keyword, Integer pageNum, Integer pageSize);

    /**
     * 查询所有客户公司（用于下拉选择）
     */
    List<CustomerEntity> listAll(Long tenantId);

    /**
     * 根据ID查询客户详情
     */
    CustomerVo.CustomerDetailVo getById(Long id, Long tenantId);

    /**
     * 创建客户公司
     */
    CustomerEntity create(CustomerVo.CustomerCreateReq req, Long tenantId, Long operatorId);

    /**
     * 更新客户公司
     */
    boolean update(Long id, CustomerVo.CustomerUpdateReq req, Long tenantId, Long operatorId);

    /**
     * 删除客户公司
     */
    boolean delete(Long id, Long tenantId);
}
