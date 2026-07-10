package com.plating.erp.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.plating.erp.biz.entity.GoodsOrderEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 货物开单主表Mapper
 *
 * @author Plating ERP Team
 */
@Mapper
public interface GoodsOrderMapper extends BaseMapper<GoodsOrderEntity> {
}
