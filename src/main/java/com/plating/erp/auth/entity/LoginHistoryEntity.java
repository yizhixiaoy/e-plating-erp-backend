package com.plating.erp.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_login_history")
public class LoginHistoryEntity {
    @TableId
    private Long id;
    private Long tenantId;
    private Long userId;
    private String loginType;
    private LocalDateTime loginTime;
    private String loginIp;
    private String deviceInfo;
    private String userAgent;
    private Integer loginStatus;
    private String failReason;
    private LocalDateTime createdAt;
}
