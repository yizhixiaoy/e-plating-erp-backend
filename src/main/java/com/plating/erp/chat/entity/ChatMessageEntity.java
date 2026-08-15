package com.plating.erp.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessageEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long conversationId;
    private Long senderId;
    /** TEXT / IMAGE / FILE / SYSTEM */
    private String msgType;
    private String content;
    private String extraJson;
    private Integer recalled;
    private LocalDateTime recalledAt;
    /** 是否已编辑 0否 1是 */
    private Integer edited;
    private LocalDateTime editedAt;
    /** 引用消息ID */
    private Long replyToId;
    /** 引用消息内容预览（最长 200） */
    private String replyPreview;
    /** 引用消息发送者名称 */
    private String replySenderName;
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;
}
