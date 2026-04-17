package com.plating.erp.audit.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_biz_log")
public class BizLogEntity {
    @TableId
    private Long id;
    private Long tenantId;
    private String bizModule;
    private Long bizId;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;
}
