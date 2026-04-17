package com.plating.erp.iam.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.iam.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户数据访问层
 * 
 * 注意：
 * - 带 @InterceptorIgnore(tenantLine = "true") 注解的方法会跳过租户拦截器
 * - 这些方法仅用于登录、Token刷新等认证场景，禁止在业务代码中使用
 * - 跳过租户拦截器后，必须在 Service 层手动校验用户所属租户
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
    
    /**
     * 根据用户名查询用户（跳过租户拦截器）
     * 仅用于登录认证场景
     * 
     * @param username 用户名
     * @return 用户实体
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0 LIMIT 1")
    UserEntity selectByUsernameForAuth(@Param("username") String username);
    
    /**
     * 根据用户ID查询用户（跳过租户拦截器）
     * 仅用于Token刷新、密码重置等认证场景
     * 
     * @param userId 用户ID
     * @return 用户实体
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_user WHERE id = #{userId} AND deleted = 0")
    UserEntity selectByIdForAuth(@Param("userId") Long userId);
    
    /**
     * 根据手机号查询用户（跳过租户拦截器）
     * 仅用于密码重置等认证场景
     * 
     * @param phone 手机号
     * @param tenantId 租户ID
     * @return 用户实体
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_user WHERE phone = #{phone} AND tenant_id = #{tenantId} AND deleted = 0 LIMIT 1")
    UserEntity selectByPhoneForAuth(@Param("phone") String phone, @Param("tenantId") Long tenantId);
}
