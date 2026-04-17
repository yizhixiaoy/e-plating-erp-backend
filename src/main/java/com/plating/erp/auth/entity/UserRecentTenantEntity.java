package com.plating.erp.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_recent_tenant")
public class UserRecentTenantEntity {
    @TableId
    private Long id;
    private Long userId;
    private Long tenantId;
    private LocalDateTime lastLoginTime;
    private Integer loginCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
