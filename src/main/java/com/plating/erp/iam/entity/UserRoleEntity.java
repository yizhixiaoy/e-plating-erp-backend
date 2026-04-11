package com.plating.erp.iam.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_user_role")
public class UserRoleEntity {
    @TableId
    private Long id;
    private Long tenantId;
    private Long userId;
    private Long roleId;
}
