package com.plating.erp.iam.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 
 * 包含用户基本信息、登录信息、审计信息等
 */
@Data
@TableName("sys_user")
public class UserEntity {
    @TableId
    private Long id;
    private Long tenantId;
    private String username;
    private String passwordHash;
    private String realName;
    private String avatarUrl;
    private Long deptId;
    private String phone;
    private String email;
    private Integer userType;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private Integer loginCount;
    private String wechatOpenid;
    private String dingtalkUserid;
    
    // 审计字段
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
