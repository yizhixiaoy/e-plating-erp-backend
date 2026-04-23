package com.plating.erp.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("msg_email_record")
public class EmailRecordEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long noticeId;
    private String receiverEmail;
    private String subject;
    private Integer sendStatus;
    private String failReason;
    private Integer retryCount;
    private LocalDateTime sentTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
