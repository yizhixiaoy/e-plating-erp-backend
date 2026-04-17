package com.plating.erp.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("auth_scan_session")
public class ScanSessionEntity {
    @TableId
    private Long id;
    private Long tenantId;
    private String qrToken;
    private String webClientId;
    private Long scannerUserId;
    private Integer sessionStatus;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
}
