package com.plating.erp.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.plating.erp.iam.entity.UserRoleEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleEntity> {
    @Delete("DELETE FROM sys_user_role WHERE tenant_id = #{tenantId} AND user_id = #{userId}")
    int deleteByUser(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_role WHERE tenant_id = #{tenantId} AND role_id = #{roleId}")
    int deleteByRole(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId);
}
