package com.plating.erp.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("msg_sms_record")
public class SmsRecordEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long receiverUserId;
    private String receiverPhone;
    private String smsType;
    private String content;
    private Integer sendStatus;
    private String failReason;
    private Integer retryCount;
    private Long operatorId;
    private LocalDateTime sentTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
