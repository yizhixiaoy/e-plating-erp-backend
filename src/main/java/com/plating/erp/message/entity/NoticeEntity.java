package com.plating.erp.message.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("msg_notice")
public class NoticeEntity {
    @TableId
    private Long id;
    private Long tenantId;
    private String noticeType;
    private String title;
    private String content;
    private Integer level;
    private String publishScope;
    private String targetJson;
    /** 0草稿 1待发布(预约) 2已发布 3已下架 4已撤回 */
    private Integer status;
    private LocalDateTime scheduledPublishAt;
    private LocalDateTime publishTime;
    private Long publishedBy;
    private LocalDateTime offlineAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
