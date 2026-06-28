package com.plating.erp.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.plating.erp.message.entity.NoticeUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface NoticeUserMapper extends BaseMapper<NoticeUserEntity> {

    @Select("<script>"
            + "SELECT COUNT(*) FROM msg_notice_user nu INNER JOIN msg_notice n ON n.id = nu.notice_id "
            + "WHERE nu.user_id = #{userId} AND IFNULL(nu.deleted,0) = 0 "
            + "AND n.status = 2 AND IFNULL(n.deleted,0) = 0 "
            + "<if test='readStatus != null'> AND nu.read_status = #{readStatus} </if>"
            + "</script>")
    Long countVisibleForUser(@Param("userId") Long userId, @Param("readStatus") Integer readStatus);

    @Select("<script>"
            + "SELECT nu.id, nu.tenant_id, nu.notice_id, nu.user_id, nu.read_status, nu.read_time, IFNULL(nu.deleted,0) AS deleted, nu.created_at, nu.updated_at "
            + "FROM msg_notice_user nu INNER JOIN msg_notice n ON n.id = nu.notice_id "
            + "WHERE nu.user_id = #{userId} AND IFNULL(nu.deleted,0) = 0 "
            + "AND n.status = 2 AND IFNULL(n.deleted,0) = 0 "
            + "<if test='readStatus != null'> AND nu.read_status = #{readStatus} </if>"
            + "ORDER BY nu.id DESC"
            + "</script>")
    IPage<NoticeUserEntity> selectVisiblePage(IPage<?> page,
                                             @Param("userId") Long userId,
                                             @Param("readStatus") Integer readStatus);

    /**
     * 增强分页：联表拼接 notice 字段返回扭平 Map（含 readStatus / title / noticeType ...）
     * 支持 noticeType / keyword / dateFrom / dateTo / readStatus 过滤
     */
    @Select("<script>"
            + "SELECT n.id AS noticeId, n.tenant_id AS tenantId, n.notice_type AS noticeType, n.title, n.content, "
            + "n.level, n.publish_scope AS publishScope, n.publish_time AS publishTime, n.created_at AS createdAt, "
            + "nu.read_status AS readStatus, nu.read_time AS readTime, nu.id AS noticeUserId "
            + "FROM msg_notice_user nu INNER JOIN msg_notice n ON n.id = nu.notice_id "
            + "WHERE nu.user_id = #{userId} AND IFNULL(nu.deleted,0) = 0 "
            + "AND n.status = 2 AND IFNULL(n.deleted,0) = 0 "
            + "<if test='readStatus != null'> AND nu.read_status = #{readStatus} </if>"
            + "<if test='noticeType != null and noticeType != \"\"'> AND n.notice_type = #{noticeType} </if>"
            + "<if test='keyword != null and keyword != \"\"'> AND (n.title LIKE CONCAT('%', #{keyword}, '%') OR n.content LIKE CONCAT('%', #{keyword}, '%')) </if>"
            + "<if test='dateFrom != null'> AND n.created_at &gt;= #{dateFrom} </if>"
            + "<if test='dateTo != null'> AND n.created_at &lt;= #{dateTo} </if>"
            + "ORDER BY nu.read_status ASC, n.created_at DESC"
            + "</script>")
    IPage<Map<String, Object>> selectMyNoticesEnhanced(IPage<?> page,
                                                      @Param("userId") Long userId,
                                                      @Param("readStatus") Integer readStatus,
                                                      @Param("noticeType") String noticeType,
                                                      @Param("keyword") String keyword,
                                                      @Param("dateFrom") LocalDateTime dateFrom,
                                                      @Param("dateTo") LocalDateTime dateTo);

    @Select("<script>"
            + "SELECT COUNT(*) FROM msg_notice_user nu INNER JOIN msg_notice n ON n.id = nu.notice_id "
            + "WHERE nu.user_id = #{userId} AND IFNULL(nu.deleted,0) = 0 "
            + "AND n.status = 2 AND IFNULL(n.deleted,0) = 0 "
            + "<if test='readStatus != null'> AND nu.read_status = #{readStatus} </if>"
            + "<if test='noticeType != null and noticeType != \"\"'> AND n.notice_type = #{noticeType} </if>"
            + "<if test='keyword != null and keyword != \"\"'> AND (n.title LIKE CONCAT('%', #{keyword}, '%') OR n.content LIKE CONCAT('%', #{keyword}, '%')) </if>"
            + "<if test='dateFrom != null'> AND n.created_at &gt;= #{dateFrom} </if>"
            + "<if test='dateTo != null'> AND n.created_at &lt;= #{dateTo} </if>"
            + "</script>")
    Long countMyNoticesEnhanced(@Param("userId") Long userId,
                                @Param("readStatus") Integer readStatus,
                                @Param("noticeType") String noticeType,
                                @Param("keyword") String keyword,
                                @Param("dateFrom") LocalDateTime dateFrom,
                                @Param("dateTo") LocalDateTime dateTo);

    /** 按 noticeType 分组统计未读数 */
    @Select("SELECT n.notice_type AS noticeType, COUNT(*) AS cnt "
            + "FROM msg_notice_user nu INNER JOIN msg_notice n ON n.id = nu.notice_id "
            + "WHERE nu.user_id = #{userId} AND IFNULL(nu.deleted,0) = 0 "
            + "AND n.status = 2 AND IFNULL(n.deleted,0) = 0 AND nu.read_status = 0 "
            + "GROUP BY n.notice_type")
    List<Map<String, Object>> countUnreadByType(@Param("userId") Long userId);

    /** 今日新增消息数 */
    @Select("SELECT COUNT(*) FROM msg_notice_user nu INNER JOIN msg_notice n ON n.id = nu.notice_id "
            + "WHERE nu.user_id = #{userId} AND IFNULL(nu.deleted,0) = 0 "
            + "AND n.status = 2 AND IFNULL(n.deleted,0) = 0 "
            + "AND DATE(n.created_at) = CURRENT_DATE")
    Long countTodayNew(@Param("userId") Long userId);

    /**
     * 批量插入通知记录
     */
    void batchInsert(@Param("list") List<NoticeUserEntity> list);
}
