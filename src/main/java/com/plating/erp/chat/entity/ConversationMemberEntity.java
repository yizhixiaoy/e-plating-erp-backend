package com.plating.erp.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_conversation_member")
public class ConversationMemberEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long conversationId;
    private Long userId;
    /** OWNER / ADMIN / MEMBER */
    private String role;
    private String nickname;
    private Integer pinned;
    private Integer muted;
    private Long lastReadId;
    private Integer unreadCount;
    private LocalDateTime joinedAt;
    private LocalDateTime quitAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
