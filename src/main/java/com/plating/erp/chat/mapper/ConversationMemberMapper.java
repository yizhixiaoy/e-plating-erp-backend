package com.plating.erp.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.plating.erp.chat.entity.ConversationMemberEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ConversationMemberMapper extends BaseMapper<ConversationMemberEntity> {

    /** 增加会话内除发送者外所有人的未读数 */
    @Update("UPDATE chat_conversation_member SET unread_count = unread_count + 1, updated_at = NOW() "
            + "WHERE conversation_id = #{conversationId} AND user_id <> #{senderId} "
            + "AND IFNULL(deleted,0) = 0 AND quit_at IS NULL")
    int incrUnreadExceptSender(@Param("conversationId") Long conversationId,
                               @Param("senderId") Long senderId);

    /** 清零某成员的未读数 + 更新最后已读消息ID */
    @Update("UPDATE chat_conversation_member SET unread_count = 0, last_read_id = #{lastReadId}, updated_at = NOW() "
            + "WHERE conversation_id = #{conversationId} AND user_id = #{userId} AND IFNULL(deleted,0) = 0")
    int markRead(@Param("conversationId") Long conversationId,
                 @Param("userId") Long userId,
                 @Param("lastReadId") Long lastReadId);
}
