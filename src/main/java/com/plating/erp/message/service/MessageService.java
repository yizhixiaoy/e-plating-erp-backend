package com.plating.erp.message.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.message.entity.EmailRecordEntity;
import com.plating.erp.message.entity.NoticeEntity;

import java.util.List;

public interface MessageService {
    PageResult<NoticeEntity> myNotices(int pageNum, int pageSize, Long userId, Integer readStatus);

    boolean markAsRead(Long noticeUserId);

    boolean publishNotice(Long noticeId);

    void deliverNoticeRecipients(Long noticeId);

    int publishDueScheduledNotices();

    PageResult<NoticeEntity> listNotices(int pageNum, int pageSize, Integer status, Long tenantId);

    NoticeEntity saveNotice(NoticeEntity entity, Long userId);

    boolean deleteNotice(Long noticeId);

    NoticeEntity getNoticeById(Long noticeId);

    NoticeEntity getNotice(Long noticeId);

    void sendSystemMessage(String title, String content, List<Long> userIds);

    PageResult<NoticeEntity> pageNotices(int pageNum, int pageSize, Long tenantId, Boolean isSystem);

    void readNotice(Long noticeId, Long userId);

    Page<EmailRecordEntity> emails(int pageNum, int pageSize, Integer sendStatus);
}
