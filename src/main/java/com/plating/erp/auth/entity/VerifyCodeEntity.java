package com.plating.erp.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("auth_verify_code")
public class VerifyCodeEntity {
    @TableId
    private Long id;
    private Long tenantId;
    private String channel;
    private String receiver;
    private String verifyCode;
    private String bizType;
    private LocalDateTime expireAt;
    private Integer usedStatus;
    private Integer failCount;
    private LocalDateTime createdAt;
    private LocalDateTime usedAt;
}
