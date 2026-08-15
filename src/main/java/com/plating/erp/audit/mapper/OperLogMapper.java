package com.plating.erp.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.plating.erp.audit.entity.OperLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperLogMapper extends BaseMapper<OperLogEntity> {
}
