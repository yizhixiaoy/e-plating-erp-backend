package com.plating.erp.chat.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天模块 VO 集合。
 */
public final class ChatVo {

    private ChatVo() {}

    /** 会话列表查询过滤 */
    public record ConvQuery(
            String convType,
            String keyword,
            Boolean unreadOnly
    ) {}

    /** 会话列表视图（联表 member 字段） */
    public record ConversationView(
            Long id,
            Long tenantId,
            String convType,
            String name,
            String avatar,
            Long ownerId,
            Long lastMessageId,
            LocalDateTime lastMessageAt,
            Integer memberCount,
            LocalDateTime createdAt,
            // 我的成员关系
            Long memberId,
            Integer pinned,
            Integer muted,
            Integer unreadCount,
            Long lastReadId,
            String memberRole,
            String memberNickname,
            // 单聊辅助：对方信息
            Long peerUserId,
            String peerName,
            String peerAvatar,
            // 最后一条消息预览
            String lastMessageType,
            String lastMessageContent,
            Long lastMessageSenderId,
            String lastMessageSenderName
    ) {}

    /** 消息视图 */
    public record MessageView(
            Long id,
            Long tenantId,
            Long conversationId,
            Long senderId,
            String senderName,
            String senderAvatar,
            String msgType,
            String content,
            String extraJson,
            Integer recalled,
            LocalDateTime recalledAt,
            Integer edited,
            LocalDateTime editedAt,
            Long replyToId,
            String replyPreview,
            String replySenderName,
            LocalDateTime createdAt
    ) {}

    /** 发送消息请求 */
    public record SendReq(
            @NotNull(message = "会话ID不能为空")
            Long conversationId,
            String msgType,
            @NotBlank(message = "内容不能为空")
            @Size(max = 4000, message = "消息内容最长 4000 字符")
            String content,
            String extraJson,
            /** 引用的消息ID（可选） */
            Long replyToId
    ) {}

    /** 编辑消息请求 */
    public record EditReq(
            @NotBlank(message = "内容不能为空")
            @Size(max = 4000, message = "消息内容最长 4000 字符")
            String content
    ) {}

    /** 已读请求 */
    public record ReadReq(
            @NotNull(message = "会话ID不能为空")
            Long conversationId,
            Long lastReadId
    ) {}

    /** 创建单聊请求 */
    public record CreateSingleReq(
            @NotNull(message = "对方用户ID不能为空")
            Long peerUserId
    ) {}

    /** 创建群聊请求 */
    public record CreateGroupReq(
            @NotBlank(message = "群名不能为空")
            @Size(max = 64, message = "群名最长 64 字符")
            String name,
            String avatar,
            @NotEmpty(message = "群成员不能为空")
            List<Long> memberIds
    ) {}

    /** 切换置顶/免打扰 */
    public record ToggleReq(
            @NotNull(message = "会话ID不能为空")
            Long conversationId,
            @NotNull(message = "目标值不能为空")
            Integer value
    ) {}

    /** 全局未读统计 */
    public record UnreadSummary(
            long total,
            long conversations
    ) {}
}
