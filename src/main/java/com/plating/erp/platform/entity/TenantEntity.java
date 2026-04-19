package com.plating.erp.platform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户实体类（SaaS多租户）
 * 
 * 包含租户基本信息、品牌配置、自定义设置、审计信息等
 */
@Data
@TableName("sys_tenant")
public class TenantEntity {
    @TableId
    private Long id;
    private String tenantName;        // 租户名称
    private String logoUrl;           // 企业Logo URL（品牌标识）
    private String shortCode;         // 租户简称（登录入口标识）
    private String contactName;       // 联系人
    private String phone;             // 联系电话
    private LocalDateTime expireTime; // 到期时间
    private Integer status;           // 状态:0正常 1冻结
    private String domain;            // 自定义域名（如 company.example.com）
    private String welcomeText;       // 租户自定义配置（JSON格式）
    
    // 审计字段
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
