package com.plating.erp.platform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_tenant")
public class TenantEntity {
    @TableId
    private Long id;
    private String tenantName;
    private String avatarUrl;
    private String shortCode;
    private String contactName;
    private String phone;
    private LocalDateTime expireTime;
    private Integer status;
    private String domain;
}
