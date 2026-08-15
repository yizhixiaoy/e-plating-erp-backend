package com.plating.erp.iam.service;

import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.iam.entity.UserEntity;

import java.util.List;

public interface UserService {
    UserEntity findByUsernameAndTenantId(String username, Long tenantId);

    UserEntity findByUsername(String username, Long tenantId);

    List<UserEntity> findByPhone(String phone, Long tenantId);

    boolean checkPassword(String rawPassword, String encodedPassword);

    boolean updatePassword(Long userId, String newPassword);

    /**
     * 分页查询用户列表
     * 
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param deptId 部门ID
     * @param status 状态
     * @param keyword 关键字（支持姓名、账号、手机号、租户名称模糊查询）
     * @param tenantId 租户ID（平台管理员使用）
     * @return 分页结果（记录包含 tenantName 租户名称）
     */
    PageResult<UserEntity> page(int pageNum, int pageSize, Long deptId, Integer status, 
                                String keyword, Long tenantId);

    /**
     * 远程搜索用户（按姓名/工号/手机号模糊匹配，仅返回在职用户）
     * 用于部门/岗位的负责人选择器
     */
    List<UserEntity> searchUsers(String keyword, Long tenantId, int limit);

    int bindRoles(Long tenantId, Long userId, List<Long> roleIds);

    boolean unbindRole(Long tenantId, Long userId, Long roleId);

    UserEntity getById(Long userId);

    boolean save(UserEntity entity);

    String updateAvatar(Long userId, org.springframework.web.multipart.MultipartFile avatarFile);

    String generateUsername(Long tenantId);

    /**
     * 获取用户绑定的角色列表
     */
    List<com.plating.erp.iam.entity.UserRoleEntity> getUserRoles(Long tenantId, Long userId);

    long count();

    /**
     * 统计指定时间之后创建的用户数量
     */
    long countSince(java.time.LocalDateTime since);
}
