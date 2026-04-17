package com.plating.erp.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.plating.erp.iam.entity.RoleMenuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenuEntity> {

    @Select("SELECT menu_id FROM sys_role_menu WHERE tenant_id = #{tenantId} AND role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId);

    int deleteByRoleId(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId);
}
