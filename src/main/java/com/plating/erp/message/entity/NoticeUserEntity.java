package com.plating.erp.message.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("msg_notice_user")
public class NoticeUserEntity {
    @TableId
    private Long id;
    private Long tenantId;
    private Long noticeId;
    private Long userId;
    private Integer readStatus;
    private LocalDateTime readTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
