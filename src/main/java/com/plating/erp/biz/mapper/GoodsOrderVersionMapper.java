package com.plating.erp.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.plating.erp.biz.entity.GoodsOrderVersionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 开单版本历史Mapper
 *
 * @author Plating ERP Team
 */
@Mapper
public interface GoodsOrderVersionMapper extends BaseMapper<GoodsOrderVersionEntity> {
}
