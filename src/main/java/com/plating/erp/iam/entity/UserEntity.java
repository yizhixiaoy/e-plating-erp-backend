package com.plating.erp.iam.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

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
}
