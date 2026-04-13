package com.plating.erp.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.plating.erp.message.entity.NoticeUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoticeUserMapper extends BaseMapper<NoticeUserEntity> {

    @Select("<script>"
            + "SELECT COUNT(*) FROM msg_notice_user nu INNER JOIN msg_notice n ON n.id = nu.notice_id "
            + "WHERE nu.user_id = #{userId} AND n.status = 2 AND IFNULL(n.deleted,0) = 0 "
            + "<if test='readStatus != null'> AND nu.read_status = #{readStatus} </if>"
            + "</script>")
    Long countVisibleForUser(@Param("userId") Long userId, @Param("readStatus") Integer readStatus);

    @Select("<script>"
            + "SELECT nu.id, nu.tenant_id, nu.notice_id, nu.user_id, nu.read_status, nu.read_time, nu.created_at, nu.updated_at "
            + "FROM msg_notice_user nu INNER JOIN msg_notice n ON n.id = nu.notice_id "
            + "WHERE nu.user_id = #{userId} AND n.status = 2 AND IFNULL(n.deleted,0) = 0 "
            + "<if test='readStatus != null'> AND nu.read_status = #{readStatus} </if>"
            + "ORDER BY nu.id DESC LIMIT #{limit} OFFSET #{offset}"
            + "</script>")
    List<NoticeUserEntity> selectVisiblePage(@Param("userId") Long userId,
                                             @Param("readStatus") Integer readStatus,
                                             @Param("offset") long offset,
                                             @Param("limit") int limit);
}
