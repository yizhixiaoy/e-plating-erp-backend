package com.plating.erp.chat.service;

import com.plating.erp.chat.entity.ChatMessageEntity;
import com.plating.erp.chat.entity.ConversationEntity;
import com.plating.erp.chat.vo.ChatVo;
import com.plating.erp.common.api.response.PageResult;

import java.util.List;

public interface ChatService {

    /** 我的会话列表 */
    PageResult<ChatVo.ConversationView> myConversations(int pageNum, int pageSize, Long userId, ChatVo.ConvQuery query);

    /** 单个会话详情（含我的成员关系） */
    ChatVo.ConversationView conversationDetail(Long conversationId, Long currentUserId);

    /** 历史消息（按 id 倒序，limit 条；beforeId 用于翻页） */
    List<ChatVo.MessageView> messages(Long conversationId, Long currentUserId, Long beforeId, int limit);

    /** 发送消息 */
    ChatMessageEntity send(ChatVo.SendReq req, Long currentUserId, Long tenantId);

    /** 已读上报：清零未读 + 记录 lastReadId */
    void markRead(ChatVo.ReadReq req, Long currentUserId);

    /** 撤回消息（2 分钟内 + 仅本人） */
    ChatMessageEntity recall(Long messageId, Long currentUserId);

    /** 编辑消息（5 分钟内 + 仅本人 + 仅 TEXT 类型） */
    ChatMessageEntity edit(Long messageId, ChatVo.EditReq req, Long currentUserId);

    /** 切换置顶 */
    void togglePinned(ChatVo.ToggleReq req, Long currentUserId);

    /** 切换免打扰 */
    void toggleMuted(ChatVo.ToggleReq req, Long currentUserId);

    /** 创建/复用单聊会话 */
    ConversationEntity createSingle(ChatVo.CreateSingleReq req, Long currentUserId, Long tenantId);

    /** 创建群聊 */
    ConversationEntity createGroup(ChatVo.CreateGroupReq req, Long currentUserId, Long tenantId);

    /** 全局未读总数（聊天） */
    ChatVo.UnreadSummary unreadSummary(Long currentUserId);

    /** 删除/退出会话（逻辑删除成员关系） */
    void deleteConversation(Long conversationId, Long currentUserId);
}
