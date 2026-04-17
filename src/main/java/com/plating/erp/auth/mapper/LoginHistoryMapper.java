package com.plating.erp.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.plating.erp.auth.entity.LoginHistoryEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginHistoryMapper extends BaseMapper<LoginHistoryEntity> {
}
