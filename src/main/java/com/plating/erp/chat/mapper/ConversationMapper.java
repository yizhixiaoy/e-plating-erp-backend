package com.plating.erp.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.plating.erp.chat.entity.ConversationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 会话 Mapper。
 * 复杂动态 SQL 见 resources/mapper/chat/ConversationMapper.xml。
 */
@Mapper
public interface ConversationMapper extends BaseMapper<ConversationEntity> {

    /** 我的会话列表（按 last_message_at 降序，置顶优先） */
    IPage<Map<String, Object>> selectMyConversations(IPage<?> page,
                                                    @Param("userId") Long userId,
                                                    @Param("convType") String convType,
                                                    @Param("keyword") String keyword,
                                                    @Param("unreadOnly") Boolean unreadOnly);

    /** 我的会话总数 */
    Long countMyConversations(@Param("userId") Long userId,
                              @Param("convType") String convType,
                              @Param("keyword") String keyword,
                              @Param("unreadOnly") Boolean unreadOnly);

    /** 单条会话视图（含成员关系 + peer + last_message） */
    Map<String, Object> selectMyConversationOne(@Param("userId") Long userId,
                                                @Param("conversationId") Long conversationId);

    /** 我的所有会话未读总数（简单 SQL，保留注解） */
    @Select("SELECT IFNULL(SUM(unread_count), 0) FROM chat_conversation_member "
            + "WHERE user_id = #{userId} AND IFNULL(deleted,0) = 0 AND quit_at IS NULL")
    Long sumMyUnread(@Param("userId") Long userId);

    /** 单聊会话查找：判断 a-b 是否已存在 SINGLE 会话（简单 SQL，保留注解） */
    @Select("SELECT c.id FROM chat_conversation c "
            + "INNER JOIN chat_conversation_member m1 ON m1.conversation_id = c.id AND m1.user_id = #{userA} AND IFNULL(m1.deleted,0) = 0 "
            + "INNER JOIN chat_conversation_member m2 ON m2.conversation_id = c.id AND m2.user_id = #{userB} AND IFNULL(m2.deleted,0) = 0 "
            + "WHERE c.conv_type = 'SINGLE' AND IFNULL(c.deleted,0) = 0 LIMIT 1")
    Long findSingleConvId(@Param("userA") Long userA, @Param("userB") Long userB);
}
