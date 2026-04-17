package com.plating.erp.iam.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体类
 */
@Data
@TableName("sys_role")
public class RoleEntity {
    @TableId
    private Long id;
    private Long tenantId;
    private String roleName;
    private String roleKey;
    private Integer dataScope;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
