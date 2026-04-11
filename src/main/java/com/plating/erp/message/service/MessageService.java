package com.plating.erp.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.message.entity.EmailRecordEntity;
import com.plating.erp.message.entity.NoticeEntity;
import com.plating.erp.message.entity.NoticeUserEntity;
import com.plating.erp.message.mapper.EmailRecordMapper;
import com.plating.erp.message.mapper.NoticeMapper;
import com.plating.erp.message.mapper.NoticeUserMapper;
import com.plating.erp.message.vo.MessageResponseVo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class MessageService {
    private final NoticeMapper noticeMapper;
    private final EmailRecordMapper emailRecordMapper;
    private final NoticeUserMapper noticeUserMapper;

    public MessageService(NoticeMapper noticeMapper, EmailRecordMapper emailRecordMapper, NoticeUserMapper noticeUserMapper) {
        this.noticeMapper = noticeMapper;
        this.emailRecordMapper = emailRecordMapper;
        this.noticeUserMapper = noticeUserMapper;
    }

    public NoticeEntity saveNotice(NoticeEntity entity, Long userId) {
        if (entity.getId() == null) {
            entity.setId(IdWorker.getId());
            noticeMapper.insert(entity);
            NoticeUserEntity nu = new NoticeUserEntity();
            nu.setId(IdWorker.getId());
            nu.setTenantId(entity.getTenantId());
            nu.setNoticeId(entity.getId());
            nu.setUserId(userId);
            nu.setReadStatus(0);
            noticeUserMapper.insert(nu);
        } else {
            noticeMapper.updateById(entity);
        }
        return entity;
    }

    public NoticeEntity getNotice(Long noticeId) {
        return noticeMapper.selectById(noticeId);
    }

    public Page<MessageResponseVo.NoticeMyItem> myNotices(int pageNum, int pageSize, Long userId, Integer readStatus) {
        LambdaQueryWrapper<NoticeUserEntity> nuQw = new LambdaQueryWrapper<>();
        nuQw.eq(NoticeUserEntity::getUserId, userId)
                .eq(readStatus != null, NoticeUserEntity::getReadStatus, readStatus)
                .orderByDesc(NoticeUserEntity::getId);
        Page<NoticeUserEntity> nuPage = noticeUserMapper.selectPage(new Page<>(pageNum, pageSize), nuQw);
        List<Long> noticeIds = nuPage.getRecords().stream().map(NoticeUserEntity::getNoticeId).toList();
        if (noticeIds.isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }
        List<NoticeEntity> notices = noticeMapper.selectList(new LambdaQueryWrapper<NoticeEntity>().in(NoticeEntity::getId, noticeIds));
        List<MessageResponseVo.NoticeMyItem> rows = nuPage.getRecords().stream().map(nu -> {
            NoticeEntity n = findNoticeById(notices, nu.getNoticeId());
            if (n == null || !Objects.equals(n.getStatus(), 2) || (n.getDeleted() != null && n.getDeleted() != 0)) {
                return null;
            }
            return new MessageResponseVo.NoticeMyItem(
                    nu.getNoticeId(),
                    n.getTitle(),
                    n.getNoticeType(),
                    nu.getReadStatus(),
                    nu.getTenantId()
            );
        }).filter(Objects::nonNull).toList();
        Page<MessageResponseVo.NoticeMyItem> result = new Page<>(pageNum, pageSize, nuPage.getTotal());
        result.setRecords(rows);
        return result;
    }

    public void readNotice(Long noticeId, Long userId) {
        NoticeUserEntity nu = noticeUserMapper.selectOne(new LambdaQueryWrapper<NoticeUserEntity>()
                .eq(NoticeUserEntity::getNoticeId, noticeId)
                .eq(NoticeUserEntity::getUserId, userId)
                .last("limit 1"));
        if (nu != null) {
            nu.setReadStatus(1);
            nu.setReadTime(LocalDateTime.now());
            noticeUserMapper.updateById(nu);
        }
    }

    public Page<EmailRecordEntity> emails(int pageNum, int pageSize, Integer sendStatus) {
        LambdaQueryWrapper<EmailRecordEntity> qw = new LambdaQueryWrapper<>();
        qw.eq(sendStatus != null, EmailRecordEntity::getSendStatus, sendStatus).orderByDesc(EmailRecordEntity::getId);
        return emailRecordMapper.selectPage(new Page<>(pageNum, pageSize), qw);
    }

    private NoticeEntity findNoticeById(List<NoticeEntity> notices, Long noticeId) {
        for (NoticeEntity notice : notices) {
            if (notice.getId().equals(noticeId)) {
                return notice;
            }
        }
        return null;
    }
}
