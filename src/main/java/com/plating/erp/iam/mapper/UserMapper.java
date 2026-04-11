package com.plating.erp.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.plating.erp.iam.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
