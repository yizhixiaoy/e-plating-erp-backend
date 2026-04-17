package com.plating.erp.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.mapper.UserMapper;
import com.plating.erp.message.entity.EmailRecordEntity;
import com.plating.erp.message.entity.NoticeEntity;
import com.plating.erp.message.entity.NoticeUserEntity;
import com.plating.erp.message.mapper.EmailRecordMapper;
import com.plating.erp.message.mapper.NoticeMapper;
import com.plating.erp.message.mapper.NoticeUserMapper;
import com.plating.erp.message.service.MessageService;
import com.plating.erp.message.service.NoticeUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {
    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

    private final NoticeMapper noticeMapper;
    private final NoticeUserMapper noticeUserMapper;
    private final NoticeUserService noticeUserService;
    private final UserMapper userMapper;
    private final EmailRecordMapper emailRecordMapper;
    private final ObjectMapper objectMapper;

    public MessageServiceImpl(NoticeMapper noticeMapper, NoticeUserMapper noticeUserMapper,
                              NoticeUserService noticeUserService, UserMapper userMapper,
                              EmailRecordMapper emailRecordMapper, ObjectMapper objectMapper) {
        this.noticeMapper = noticeMapper;
        this.noticeUserMapper = noticeUserMapper;
        this.noticeUserService = noticeUserService;
        this.userMapper = userMapper;
        this.emailRecordMapper = emailRecordMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public PageResult<NoticeEntity> myNotices(int pageNum, int pageSize, Long userId, Integer readStatus) {
        long offset = (pageNum - 1L) * pageSize;
        List<NoticeUserEntity> noticeUsers = noticeUserMapper.selectVisiblePage(userId, readStatus, offset, pageSize);
        Long total = noticeUserMapper.countVisibleForUser(userId, readStatus);

        List<Long> noticeIds = noticeUsers.stream()
                .map(NoticeUserEntity::getNoticeId)
                .collect(Collectors.toList());
        List<NoticeEntity> notices = new ArrayList<>();
        if (!noticeIds.isEmpty()) {
            notices = noticeMapper.selectBatchIds(noticeIds);
            Map<Long, NoticeEntity> noticeMap = notices.stream()
                    .collect(Collectors.toMap(NoticeEntity::getId, n -> n));
            List<NoticeEntity> ordered = new ArrayList<>();
            for (NoticeUserEntity nu : noticeUsers) {
                NoticeEntity notice = noticeMap.get(nu.getNoticeId());
                if (notice != null) {
                    ordered.add(notice);
                }
            }
            notices = ordered;
        }

        return new PageResult<>(notices, total);
    }

    @Override
    public boolean markAsRead(Long noticeUserId) {
        NoticeUserEntity noticeUser = noticeUserMapper.selectById(noticeUserId);
        if (noticeUser == null) {
            return false;
        }
        noticeUser.setReadStatus(1);
        noticeUser.setReadTime(LocalDateTime.now());
        noticeUserMapper.updateById(noticeUser);
        return true;
    }

    @Override
    public boolean publishNotice(Long noticeId) {
        NoticeEntity notice = noticeMapper.selectById(noticeId);
        if (notice == null || !Objects.equals(notice.getStatus(), 1)) {
            return false;
        }

        notice.setStatus(2);
        notice.setPublishTime(LocalDateTime.now());
        noticeMapper.updateById(notice);

        deliverNoticeRecipients(noticeId);
        return true;
    }

    @Override
    public void deliverNoticeRecipients(Long noticeId) {
        NoticeEntity notice = noticeMapper.selectById(noticeId);
        if (notice == null || !Objects.equals(notice.getStatus(), 2)) {
            return;
        }

        List<Long> userIds = resolveRecipientUserIds(notice);
        if (userIds.isEmpty()) {
            log.warn("notice {} has no recipients after resolve", noticeId);
            return;
        }

        List<NoticeUserEntity> existingRecords = noticeUserMapper.selectList(
                new LambdaQueryWrapper<NoticeUserEntity>()
                        .eq(NoticeUserEntity::getNoticeId, noticeId)
                        .in(NoticeUserEntity::getUserId, userIds)
        );

        Set<Long> existingUserIds = new HashSet<>();
        for (NoticeUserEntity nu : existingRecords) {
            existingUserIds.add(nu.getUserId());
        }

        List<NoticeUserEntity> newRecords = new ArrayList<>();
        for (Long uid : userIds) {
            if (existingUserIds.contains(uid)) {
                continue;
            }
            UserEntity u = userMapper.selectById(uid);
            NoticeUserEntity nu = new NoticeUserEntity();
            nu.setId(System.currentTimeMillis() + uid);
            nu.setTenantId(u != null ? u.getTenantId() : notice.getTenantId());
            nu.setNoticeId(noticeId);
            nu.setUserId(uid);
            nu.setReadStatus(0);
            nu.setCreatedAt(LocalDateTime.now());
            nu.setUpdatedAt(LocalDateTime.now());
            newRecords.add(nu);
        }

        if (!newRecords.isEmpty()) {
            noticeUserService.saveBatch(newRecords, 1000);
        }
    }

    @Transactional
    @Override
    public int publishDueScheduledNotices() {
        List<NoticeEntity> due = noticeMapper.selectList(new LambdaQueryWrapper<NoticeEntity>()
                .eq(NoticeEntity::getStatus, 1)
                .isNotNull(NoticeEntity::getScheduledPublishAt)
                .le(NoticeEntity::getScheduledPublishAt, LocalDateTime.now()));
        int n = 0;
        for (NoticeEntity found : due) {
            found.setStatus(2);
            found.setPublishTime(LocalDateTime.now());
            found.setPublishedBy(null);
            found.setScheduledPublishAt(null);
            found.setOfflineAt(null);
            noticeMapper.updateById(found);
            deliverNoticeRecipients(found.getId());
            n++;
        }
        return n;
    }

    private List<Long> resolveRecipientUserIds(NoticeEntity notice) {
        String scope = notice.getPublishScope() == null ? "TENANT" : notice.getPublishScope();
        JsonNode root = parseTargetJson(notice.getTargetJson());
        try {
            return switch (scope) {
                case "PLATFORM" -> resolvePlatform(root);
                case "DEPT" -> resolveDept(notice, root);
                case "USER" -> resolveUser(root, notice.getTenantId());
                case "TENANT" -> resolveTenant(notice, root);
                default -> resolveTenant(notice, root);
            };
        } catch (Exception e) {
            log.warn("resolve recipients failed for notice {}, fallback TENANT", notice.getId(), e);
            return usersInTenantIds(List.of(notice.getTenantId()));
        }
    }

    private List<Long> resolvePlatform(JsonNode root) {
        List<Long> tenantIds = readLongArray(root, "tenantIds");
        if (!tenantIds.isEmpty()) {
            return usersInTenantIds(tenantIds);
        }
        return allActiveUserIds();
    }

    private List<Long> resolveTenant(NoticeEntity notice, JsonNode root) {
        List<Long> tenantIds = readLongArray(root, "tenantIds");
        if (!tenantIds.isEmpty()) {
            return usersInTenantIds(tenantIds);
        }
        return usersInTenantIds(List.of(notice.getTenantId()));
    }

    private List<Long> resolveDept(NoticeEntity notice, JsonNode root) {
        List<Long> deptIds = readLongArray(root, "deptIds");
        if (deptIds.isEmpty()) {
            return usersInTenantIds(List.of(notice.getTenantId()));
        }
        return userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getTenantId, notice.getTenantId())
                        .in(UserEntity::getDeptId, deptIds)
                        .eq(UserEntity::getStatus, 0))
                .stream()
                .map(UserEntity::getId)
                .distinct()
                .toList();
    }

    private List<Long> resolveUser(JsonNode root, Long defaultTenantId) {
        List<Long> userIds = readLongArray(root, "userIds");
        if (userIds.isEmpty()) {
            return List.of();
        }
        List<UserEntity> users = userMapper.selectList(new LambdaQueryWrapper<UserEntity>().in(UserEntity::getId, userIds));
        List<Long> ok = new ArrayList<>();
        for (UserEntity u : users) {
            if (u.getStatus() != null && u.getStatus() != 0) {
                continue;
            }
            ok.add(u.getId());
        }
        return ok;
    }

    private List<Long> usersInTenantIds(List<Long> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                        .in(UserEntity::getTenantId, tenantIds)
                        .eq(UserEntity::getStatus, 0))
                .stream()
                .map(UserEntity::getId)
                .distinct()
                .toList();
    }

    private List<Long> allActiveUserIds() {
        return userMapper.selectList(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getStatus, 0))
                .stream()
                .map(UserEntity::getId)
                .distinct()
                .toList();
    }

    private JsonNode parseTargetJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return objectMapper.readTree("{}");
            }
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private List<Long> readLongArray(JsonNode root, String field) {
        List<Long> out = new ArrayList<>();
        if (root == null || !root.has(field) || !root.get(field).isArray()) {
            return out;
        }
        for (JsonNode n : root.get(field)) {
            if (n.isNumber()) {
                out.add(n.longValue());
            }
        }
        return out;
    }

    @Override
    public PageResult<NoticeEntity> listNotices(int pageNum, int pageSize, Integer status, Long tenantId) {
        LambdaQueryWrapper<NoticeEntity> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            wrapper.eq(NoticeEntity::getTenantId, tenantId);
        }
        if (status != null) {
            wrapper.eq(NoticeEntity::getStatus, status);
        }
        wrapper.orderByDesc(NoticeEntity::getId);

        Page<NoticeEntity> page = noticeMapper.selectPage(
                new Page<>(pageNum, pageSize),
                wrapper
        );

        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    @Override
    public NoticeEntity saveNotice(NoticeEntity entity, Long userId) {
        if (entity.getId() == null) {
            entity.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
        }
        entity.setUpdatedBy(userId);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        if (noticeMapper.selectById(entity.getId()) != null) {
            noticeMapper.updateById(entity);
        } else {
            noticeMapper.insert(entity);
        }
        return entity;
    }

    @Override
    public boolean deleteNotice(Long noticeId) {
        NoticeEntity notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            return false;
        }
        noticeMapper.deleteById(noticeId);
        return true;
    }

    @Override
    public NoticeEntity getNoticeById(Long noticeId) {
        return noticeMapper.selectById(noticeId);
    }

    @Override
    public NoticeEntity getNotice(Long noticeId) {
        return noticeMapper.selectById(noticeId);
    }

    @Override
    public void sendSystemMessage(String title, String content, List<Long> userIds) {
        NoticeEntity notice = new NoticeEntity();
        notice.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
        notice.setTenantId(0L);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setPublishScope("USER");
        notice.setStatus(2);
        notice.setPublishTime(LocalDateTime.now());
        notice.setCreatedAt(LocalDateTime.now());
        notice.setUpdatedAt(LocalDateTime.now());
        noticeMapper.insert(notice);

        deliverNoticeRecipients(notice.getId());
    }

    @Override
    public PageResult<NoticeEntity> pageNotices(int pageNum, int pageSize, Long tenantId, Boolean isSystem) {
        LambdaQueryWrapper<NoticeEntity> wrapper = new LambdaQueryWrapper<>();
        if (!Boolean.TRUE.equals(isSystem)) {
            wrapper.eq(NoticeEntity::getTenantId, tenantId);
        }
        wrapper.orderByDesc(NoticeEntity::getId);

        Page<NoticeEntity> page = noticeMapper.selectPage(
                new Page<>(pageNum, pageSize),
                wrapper
        );

        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    @Override
    public void readNotice(Long noticeId, Long userId) {
        NoticeUserEntity noticeUser = noticeUserMapper.selectOne(
                new LambdaQueryWrapper<NoticeUserEntity>()
                        .eq(NoticeUserEntity::getNoticeId, noticeId)
                        .eq(NoticeUserEntity::getUserId, userId)
        );
        if (noticeUser != null && noticeUser.getReadStatus() == 0) {
            noticeUser.setReadStatus(1);
            noticeUser.setReadTime(LocalDateTime.now());
            noticeUserMapper.updateById(noticeUser);
        }
    }

    @Override
    public Page<EmailRecordEntity> emails(int pageNum, int pageSize, Integer sendStatus) {
        LambdaQueryWrapper<EmailRecordEntity> wrapper = new LambdaQueryWrapper<>();
        if (sendStatus != null) {
            wrapper.eq(EmailRecordEntity::getSendStatus, sendStatus);
        }
        wrapper.orderByDesc(EmailRecordEntity::getId);
        return emailRecordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }
}
