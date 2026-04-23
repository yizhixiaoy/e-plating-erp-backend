package com.plating.erp.iam.service;

import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.iam.entity.UserEntity;
import org.springframework.web.multipart.MultipartFile;

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
     * @param keyword 关键字（支持姓名、账号、手机号、租户名称模糊查询）
     * @param tenantId 租户ID（平台管理员使用）
     * @return 分页结果（记录包含 tenantName 租户名称）
     */
    PageResult<UserEntity> page(int pageNum, int pageSize, Long deptId, Integer status, 
                                String keyword, Long tenantId);

    int bindRoles(Long tenantId, Long userId, List<Long> roleIds);

    boolean unbindRole(Long tenantId, Long userId, Long roleId);

    UserEntity getById(Long userId);

    boolean save(UserEntity entity);

    /**
     * 更新用户头像
     * @param userId 用户ID
     * @param avatarFile 头像文件
     * @return OSS路径（已URLEncode编码）
     */
    String updateAvatar(Long userId, MultipartFile avatarFile);

    /**
     * 根据租户ID生成下一个可用账号
     * 规则：租户简称 + "-" + 4位序号，如 ZD-0001
     * @param tenantId 租户ID
     * @return 生成的账号
     */
    String generateUsername(Long tenantId);
}
