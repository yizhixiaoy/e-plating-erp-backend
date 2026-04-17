package com.plating.erp.iam.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_role_menu")
public class RoleMenuEntity {
    @TableId
    private Long id;
    private Long tenantId;
    private Long roleId;
    private Long menuId;
    private Long createdBy;
    private LocalDateTime createdAt;
}
