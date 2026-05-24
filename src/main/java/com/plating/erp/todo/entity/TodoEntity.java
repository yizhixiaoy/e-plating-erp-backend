package com.plating.erp.todo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_todo")
public class TodoEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long assigneeId;
    private Long creatorId;
    private String todoType;
    private Integer priority;
    private String title;
    private String content;
    private String bizModule;
    private String bizRefId;
    private String bizRefUrl;
    /** 0待处理 1已处理 2已忽略 3已转交 */
    private Integer status;
    /** AGREE/REJECT/TRANSFER/COMPLETE */
    private String handleAction;
    private String handleRemark;
    private LocalDateTime handledAt;
    private LocalDateTime deadline;
    private Integer readStatus;
    private LocalDateTime readTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
    /** 是否邮件推送:0否 1是 */
    private Integer pushEmail;
}
