package com.plating.erp.platform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户实体类
 * 
 * 包含租户基本信息、审计信息等
 */
@Data
@TableName("sys_tenant")
public class TenantEntity {
    @TableId
    private Long id;
    private String tenantName;
    private String avatarUrl;
    private String logoUrl;
    private String shortCode;
    private String contactName;
    private String phone;
    private LocalDateTime expireTime;
    private Integer status;
    private String domain;
    private String welcomeText;
    
    // 审计字段
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
