package com.plating.erp.iam.service;

import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.iam.entity.UserEntity;

import java.util.List;

public interface UserService {
    UserEntity findByUsernameAndTenantId(String username, Long tenantId);

    UserEntity findByUsername(String username, Long tenantId);

    UserEntity findByPhone(String phone, Long tenantId);

    boolean checkPassword(String rawPassword, String encodedPassword);

    boolean updatePassword(Long userId, String newPassword);

    /**
     * 分页查询用户列表
     * 
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param deptId 部门ID
     * @param status 状态
     * @param keyword 关键字（姓名/账号/手机号）
     * @param tenantId 租户ID（平台管理员使用）
     * @param isSystem 是否平台管理员
     * @return 分页结果
     */
    PageResult<UserEntity> page(int pageNum, int pageSize, Long deptId, Integer status, 
                                String keyword, Long tenantId, Boolean isSystem);

    int bindRoles(Long tenantId, Long userId, List<Long> roleIds);

    boolean unbindRole(Long tenantId, Long userId, Long roleId);

    UserEntity getById(Long userId);

    boolean save(UserEntity entity);
}
