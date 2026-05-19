package com.plating.erp.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.plating.erp.chat.entity.ChatMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 聊天消息 Mapper。
 * 复杂联表 SQL 见 resources/mapper/chat/ChatMessageMapper.xml。
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageEntity> {

    /**
     * 历史消息分页（按 id 倒序）
     * 联表 sys_user 拼接发送者昵称/头像；联表 chat_message + sys_user 兜底引用消息发送者名称。
     */
    List<Map<String, Object>> selectMessagesPage(@Param("conversationId") Long conversationId,
                                                 @Param("beforeId") Long beforeId,
                                                 @Param("limit") int limit);
}
