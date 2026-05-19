package com.plating.erp.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.plating.erp.chat.entity.ChatMessageEntity;
import com.plating.erp.chat.entity.ConversationEntity;
import com.plating.erp.chat.entity.ConversationMemberEntity;
import com.plating.erp.chat.mapper.ChatMessageMapper;
import com.plating.erp.chat.mapper.ConversationMapper;
import com.plating.erp.chat.mapper.ConversationMemberMapper;
import com.plating.erp.chat.service.ChatService;
import com.plating.erp.chat.vo.ChatVo;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 聊天 Service 实现。
 * - 单聊：复用已存在的 SINGLE 会话；
 * - 群聊：创建者默认为 OWNER；
 * - 发送消息：维护 conversation.last_message_id/last_message_at，并增加除发送者外其他成员的 unread；
 * - 已读：清零 unread_count 并写 last_read_id；
 * - 撤回：仅允许 2 分钟内 + 仅本人。
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final long RECALL_LIMIT_MINUTES = 2L;
    private static final long EDIT_LIMIT_MINUTES = 5L;
    private static final int REPLY_PREVIEW_MAX = 200;
    private static final Set<String> VALID_MSG_TYPES = Set.of("TEXT", "IMAGE", "FILE", "SYSTEM");

    private final ConversationMapper conversationMapper;
    private final ConversationMemberMapper memberMapper;
    private final ChatMessageMapper messageMapper;
    private final UserMapper userMapper;

    public ChatServiceImpl(ConversationMapper conversationMapper,
                           ConversationMemberMapper memberMapper,
                           ChatMessageMapper messageMapper,
                           UserMapper userMapper) {
        this.conversationMapper = conversationMapper;
        this.memberMapper = memberMapper;
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
    }

    @Override
    public PageResult<ChatVo.ConversationView> myConversations(int pageNum, int pageSize, Long userId, ChatVo.ConvQuery query) {
        long offset = (pageNum - 1L) * pageSize;
        String convType = query == null ? null : query.convType();
        String keyword = query == null ? null : query.keyword();
        Boolean unreadOnly = query == null ? null : query.unreadOnly();
        List<Map<String, Object>> rows = conversationMapper.selectMyConversations(
                userId, convType, keyword, unreadOnly, offset, pageSize);
        Long total = conversationMapper.countMyConversations(userId, convType, keyword, unreadOnly);
        List<ChatVo.ConversationView> records = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            records.add(toView(row));
        }
        return new PageResult<>(records, total == null ? 0L : total);
    }

    @Override
    public ChatVo.ConversationView conversationDetail(Long conversationId, Long currentUserId) {
        Map<String, Object> row = conversationMapper.selectMyConversationOne(currentUserId, conversationId);
        if (row == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "会话不存在或您不是成员");
        }
        return toView(row);
    }

    @Override
    public List<ChatVo.MessageView> messages(Long conversationId, Long currentUserId, Long beforeId, int limit) {
        ensureMember(conversationId, currentUserId);
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<Map<String, Object>> rows = messageMapper.selectMessagesPage(conversationId, beforeId, safeLimit);
        List<ChatVo.MessageView> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            result.add(new ChatVo.MessageView(
                    asLong(row.get("id")),
                    asLong(row.get("tenantId")),
                    asLong(row.get("conversationId")),
                    asLong(row.get("senderId")),
                    asString(row.get("senderName")),
                    asString(row.get("senderAvatar")),
                    asString(row.get("msgType")),
                    asString(row.get("content")),
                    asString(row.get("extraJson")),
                    asInt(row.get("recalled")),
                    asDateTime(row.get("recalledAt")),
                    asInt(row.get("edited")),
                    asDateTime(row.get("editedAt")),
                    asLong(row.get("replyToId")),
                    asString(row.get("replyPreview")),
                    asString(row.get("replySenderName")),
                    asDateTime(row.get("createdAt"))
            ));
        }
        // 按时间正序返回（前端可直接渲染）
        Collections.reverse(result);
        return result;
    }

    @Override
    @Transactional
    public ChatMessageEntity send(ChatVo.SendReq req, Long currentUserId, Long tenantId) {
        ConversationMemberEntity me = ensureMember(req.conversationId(), currentUserId);
        String msgType = req.msgType() == null ? "TEXT" : req.msgType().toUpperCase();
        if (!VALID_MSG_TYPES.contains(msgType)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "非法的消息类型");
        }
        ChatMessageEntity msg = new ChatMessageEntity();
        msg.setTenantId(tenantId == null ? 0L : tenantId);
        msg.setConversationId(req.conversationId());
        msg.setSenderId(currentUserId);
        msg.setMsgType(msgType);
        msg.setContent(req.content());
        msg.setExtraJson(req.extraJson());
        msg.setRecalled(0);
        msg.setEdited(0);
        // 处理引用回复：验证被引用消息在同一会话且未被删除/撤回，并冗余预览
        if (req.replyToId() != null) {
            ChatMessageEntity replyTo = messageMapper.selectById(req.replyToId());
            if (replyTo == null || !req.conversationId().equals(replyTo.getConversationId())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "被引用消息不存在");
            }
            if (replyTo.getRecalled() != null && replyTo.getRecalled() == 1) {
                throw new BizException(ErrorCode.BAD_REQUEST, "被引用消息已撤回");
            }
            msg.setReplyToId(replyTo.getId());
            msg.setReplyPreview(buildPreview(replyTo));
            msg.setReplySenderName(querySenderDisplayName(replyTo.getSenderId()));
        }
        msg.setCreatedAt(LocalDateTime.now());
        msg.setDeleted(0);
        messageMapper.insert(msg);

        // 维护会话最新消息
        ConversationEntity conv = conversationMapper.selectById(req.conversationId());
        if (conv != null) {
            conv.setLastMessageId(msg.getId());
            conv.setLastMessageAt(msg.getCreatedAt());
            conv.setUpdatedAt(LocalDateTime.now());
            conversationMapper.updateById(conv);
        }

        // 其他成员未读 +1；发送者本人保持 0 并刷新 last_read_id
        memberMapper.incrUnreadExceptSender(req.conversationId(), currentUserId);
        memberMapper.markRead(req.conversationId(), currentUserId, msg.getId());
        // 抑制 me 未使用告警
        if (me.getQuitAt() != null) {
            throw new BizException(ErrorCode.FORBIDDEN, "您已退出该会话");
        }
        return msg;
    }

    @Override
    @Transactional
    public void markRead(ChatVo.ReadReq req, Long currentUserId) {
        ensureMember(req.conversationId(), currentUserId);
        Long lastReadId = req.lastReadId();
        if (lastReadId == null) {
            // 取该会话最大消息 id
            ChatMessageEntity last = messageMapper.selectOne(new LambdaQueryWrapper<ChatMessageEntity>()
                    .eq(ChatMessageEntity::getConversationId, req.conversationId())
                    .orderByDesc(ChatMessageEntity::getId)
                    .last("LIMIT 1"));
            lastReadId = last == null ? 0L : last.getId();
        }
        memberMapper.markRead(req.conversationId(), currentUserId, lastReadId);
    }

    @Override
    @Transactional
    public ChatMessageEntity recall(Long messageId, Long currentUserId) {
        ChatMessageEntity msg = messageMapper.selectById(messageId);
        if (msg == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "消息不存在");
        }
        if (msg.getSenderId() == null || !msg.getSenderId().equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅可撤回自己的消息");
        }
        if (msg.getRecalled() != null && msg.getRecalled() == 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "消息已撤回");
        }
        if (msg.getCreatedAt() == null
                || ChronoUnit.MINUTES.between(msg.getCreatedAt(), LocalDateTime.now()) > RECALL_LIMIT_MINUTES) {
            throw new BizException(ErrorCode.BAD_REQUEST, "超过 2 分钟不可撤回");
        }
        msg.setRecalled(1);
        msg.setRecalledAt(LocalDateTime.now());
        messageMapper.updateById(msg);
        return msg;
    }

    @Override
    @Transactional
    public ChatMessageEntity edit(Long messageId, ChatVo.EditReq req, Long currentUserId) {
        ChatMessageEntity msg = messageMapper.selectById(messageId);
        if (msg == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "消息不存在");
        }
        if (msg.getSenderId() == null || !msg.getSenderId().equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅可编辑自己的消息");
        }
        if (msg.getRecalled() != null && msg.getRecalled() == 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "已撤回的消息不可编辑");
        }
        if (!"TEXT".equalsIgnoreCase(msg.getMsgType())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仅文本消息可编辑");
        }
        if (msg.getCreatedAt() == null
                || ChronoUnit.MINUTES.between(msg.getCreatedAt(), LocalDateTime.now()) > EDIT_LIMIT_MINUTES) {
            throw new BizException(ErrorCode.BAD_REQUEST, "超过 " + EDIT_LIMIT_MINUTES + " 分钟不可编辑");
        }
        if (req.content() == null || req.content().equals(msg.getContent())) {
            return msg;
        }
        msg.setContent(req.content());
        msg.setEdited(1);
        msg.setEditedAt(LocalDateTime.now());
        messageMapper.updateById(msg);
        // 如果是会话最后一条消息，同步更新会话预览不变（lastMessageId/at 未变）
        return msg;
    }

    @Override
    @Transactional
    public void togglePinned(ChatVo.ToggleReq req, Long currentUserId) {
        ConversationMemberEntity me = ensureMember(req.conversationId(), currentUserId);
        me.setPinned(req.value() == null || req.value() == 0 ? 0 : 1);
        me.setUpdatedAt(LocalDateTime.now());
        memberMapper.updateById(me);
    }

    @Override
    @Transactional
    public void toggleMuted(ChatVo.ToggleReq req, Long currentUserId) {
        ConversationMemberEntity me = ensureMember(req.conversationId(), currentUserId);
        me.setMuted(req.value() == null || req.value() == 0 ? 0 : 1);
        me.setUpdatedAt(LocalDateTime.now());
        memberMapper.updateById(me);
    }

    @Override
    @Transactional
    public ConversationEntity createSingle(ChatVo.CreateSingleReq req, Long currentUserId, Long tenantId) {
        if (req.peerUserId() == null || req.peerUserId().equals(currentUserId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "对方用户非法");
        }
        Long existingId = conversationMapper.findSingleConvId(currentUserId, req.peerUserId());
        if (existingId != null) {
            return conversationMapper.selectById(existingId);
        }
        ConversationEntity conv = new ConversationEntity();
        conv.setTenantId(tenantId == null ? 0L : tenantId);
        conv.setConvType("SINGLE");
        conv.setMemberCount(2);
        conv.setCreatedBy(currentUserId);
        conv.setCreatedAt(LocalDateTime.now());
        conv.setUpdatedAt(LocalDateTime.now());
        conv.setDeleted(0);
        conversationMapper.insert(conv);

        addMember(conv.getId(), currentUserId, "MEMBER", tenantId);
        addMember(conv.getId(), req.peerUserId(), "MEMBER", tenantId);
        return conv;
    }

    @Override
    @Transactional
    public ConversationEntity createGroup(ChatVo.CreateGroupReq req, Long currentUserId, Long tenantId) {
        if (req.memberIds() == null || req.memberIds().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "群成员不能为空");
        }
        // 去重并加入创建者
        Set<Long> memberSet = new HashSet<>(req.memberIds());
        memberSet.add(currentUserId);

        ConversationEntity conv = new ConversationEntity();
        conv.setTenantId(tenantId == null ? 0L : tenantId);
        conv.setConvType("GROUP");
        conv.setName(req.name());
        conv.setAvatar(req.avatar());
        conv.setOwnerId(currentUserId);
        conv.setMemberCount(memberSet.size());
        conv.setCreatedBy(currentUserId);
        conv.setCreatedAt(LocalDateTime.now());
        conv.setUpdatedAt(LocalDateTime.now());
        conv.setDeleted(0);
        conversationMapper.insert(conv);

        for (Long uid : memberSet) {
            String role = uid.equals(currentUserId) ? "OWNER" : "MEMBER";
            addMember(conv.getId(), uid, role, tenantId);
        }
        return conv;
    }

    @Override
    public ChatVo.UnreadSummary unreadSummary(Long currentUserId) {
        Long total = conversationMapper.sumMyUnread(currentUserId);
        Long convs = memberMapper.selectCount(new LambdaQueryWrapper<ConversationMemberEntity>()
                .eq(ConversationMemberEntity::getUserId, currentUserId)
                .gt(ConversationMemberEntity::getUnreadCount, 0));
        return new ChatVo.UnreadSummary(
                total == null ? 0L : total,
                convs == null ? 0L : convs);
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId, Long currentUserId) {
        ConversationMemberEntity me = memberMapper.selectOne(new LambdaQueryWrapper<ConversationMemberEntity>()
                .eq(ConversationMemberEntity::getConversationId, conversationId)
                .eq(ConversationMemberEntity::getUserId, currentUserId)
                .last("LIMIT 1"));
        if (me == null) {
            throw new BizException(ErrorCode.FORBIDDEN, "您不是该会话的成员");
        }
        // 逻辑删除成员关系
        me.setDeleted(1);
        me.setQuitAt(LocalDateTime.now());
        me.setUpdatedAt(LocalDateTime.now());
        memberMapper.updateById(me);
    }

    // ==================== 私有工具 ====================

    private ConversationMemberEntity ensureMember(Long conversationId, Long userId) {
        ConversationMemberEntity me = memberMapper.selectOne(new LambdaQueryWrapper<ConversationMemberEntity>()
                .eq(ConversationMemberEntity::getConversationId, conversationId)
                .eq(ConversationMemberEntity::getUserId, userId)
                .last("LIMIT 1"));
        if (me == null) {
            throw new BizException(ErrorCode.FORBIDDEN, "您不是该会话的成员");
        }
        return me;
    }

    private static String buildPreview(ChatMessageEntity msg) {
        String content = msg.getContent();
        String type = msg.getMsgType() == null ? "TEXT" : msg.getMsgType();
        String text;
        switch (type) {
            case "IMAGE" -> text = "[图片]";
            case "FILE" -> text = "[文件]";
            case "SYSTEM" -> text = "[系统消息]";
            default -> text = content == null ? "" : content;
        }
        if (text.length() > REPLY_PREVIEW_MAX) {
            text = text.substring(0, REPLY_PREVIEW_MAX);
        }
        return text;
    }

    private String querySenderDisplayName(Long senderId) {
        if (senderId == null) return null;
        UserEntity user = userMapper.selectById(senderId);
        if (user == null) return null;
        return user.getRealName() != null && !user.getRealName().isEmpty()
                ? user.getRealName()
                : user.getUsername();
    }

    private void addMember(Long conversationId, Long userId, String role, Long tenantId) {
        ConversationMemberEntity m = new ConversationMemberEntity();
        m.setTenantId(tenantId == null ? 0L : tenantId);
        m.setConversationId(conversationId);
        m.setUserId(userId);
        m.setRole(role);
        m.setPinned(0);
        m.setMuted(0);
        m.setUnreadCount(0);
        m.setJoinedAt(LocalDateTime.now());
        m.setCreatedAt(LocalDateTime.now());
        m.setUpdatedAt(LocalDateTime.now());
        m.setDeleted(0);
        memberMapper.insert(m);
    }

    private ChatVo.ConversationView toView(Map<String, Object> row) {
        return new ChatVo.ConversationView(
                asLong(row.get("id")),
                asLong(row.get("tenantId")),
                asString(row.get("convType")),
                asString(row.get("name")),
                asString(row.get("avatar")),
                asLong(row.get("ownerId")),
                asLong(row.get("lastMessageId")),
                asDateTime(row.get("lastMessageAt")),
                asInt(row.get("memberCount")),
                asDateTime(row.get("createdAt")),
                asLong(row.get("memberId")),
                asInt(row.get("pinned")),
                asInt(row.get("muted")),
                asInt(row.get("unreadCount")),
                asLong(row.get("lastReadId")),
                asString(row.get("memberRole")),
                asString(row.get("memberNickname")),
                asLong(row.get("peerUserId")),
                asString(row.get("peerName")),
                asString(row.get("peerAvatar")),
                asString(row.get("lastMessageType")),
                asString(row.get("lastMessageContent")),
                asLong(row.get("lastMessageSenderId")),
                asString(row.get("lastMessageSenderName"))
        );
    }

    private static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.valueOf(v.toString());
    }

    private static Integer asInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        return Integer.valueOf(v.toString());
    }

    private static String asString(Object v) {
        return v == null ? null : v.toString();
    }

    private static LocalDateTime asDateTime(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDateTime dt) return dt;
        if (v instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        return null;
    }
}
