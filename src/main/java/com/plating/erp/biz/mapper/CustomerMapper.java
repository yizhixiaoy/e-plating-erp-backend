package com.plating.erp.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.plating.erp.biz.entity.CustomerEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户公司Mapper
 *
 * @author Plating ERP Team
 */
@Mapper
public interface CustomerMapper extends BaseMapper<CustomerEntity> {
}
