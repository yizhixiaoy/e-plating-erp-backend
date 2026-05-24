package com.plating.erp.iam.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.iam.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

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
    
    /**
     * 更新用户登录信息（跳过租户拦截器）
     * 仅用于登录成功后更新登录统计
     * 
     * @param userId 用户ID
     * @param lastLoginAt 最后登录时间
     * @param loginCount 登录次数
     * @param lastLoginIp 最后登录IP
     * @return 影响行数
     */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE sys_user SET last_login_at = #{lastLoginAt}, login_count = #{loginCount}, " +
            "last_login_ip = #{lastLoginIp} WHERE id = #{userId} AND deleted = 0")
    int updateLoginInfo(@Param("userId") Long userId, 
                        @Param("lastLoginAt") LocalDateTime lastLoginAt,
                        @Param("loginCount") Integer loginCount,
                        @Param("lastLoginIp") String lastLoginIp);
    
    /**
     * 分页查询用户（支持租户名称模糊查询）
     * 
     * @param page 分页对象
     * @param tenantId 租户ID（非平台用户必传）
     * @param deptId 部门ID（可选）
     * @param status 状态（可选）
     * @param keyword 关键字（支持姓名、账号、手机号、租户名称模糊查询）
     * @return 分页结果
     */
    IPage<UserEntity> selectPageWithTenantName(Page<UserEntity> page,
                                               @Param("tenantId") Long tenantId,
                                               @Param("deptId") Long deptId,
                                               @Param("status") Integer status,
                                               @Param("keyword") String keyword);

    /**
     * 检查邮箱是否已存在
     */
    default boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getEmail, email)) > 0;
    }
}
