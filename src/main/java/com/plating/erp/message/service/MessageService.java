package com.plating.erp.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.mapper.UserMapper;
import com.plating.erp.message.entity.EmailRecordEntity;
import com.plating.erp.message.entity.NoticeEntity;
import com.plating.erp.message.entity.NoticeUserEntity;
import com.plating.erp.message.mapper.EmailRecordMapper;
import com.plating.erp.message.mapper.NoticeMapper;
import com.plating.erp.message.mapper.NoticeUserMapper;
import com.plating.erp.message.vo.MessageResponseVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class MessageService {
    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final NoticeMapper noticeMapper;
    private final EmailRecordMapper emailRecordMapper;
    private final NoticeUserMapper noticeUserMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public MessageService(NoticeMapper noticeMapper,
                          EmailRecordMapper emailRecordMapper,
                          NoticeUserMapper noticeUserMapper,
                          UserMapper userMapper,
                          ObjectMapper objectMapper) {
        this.noticeMapper = noticeMapper;
        this.emailRecordMapper = emailRecordMapper;
        this.noticeUserMapper = noticeUserMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    public NoticeEntity saveNotice(NoticeEntity entity, Long userId) {
        if (entity.getId() == null) {
            entity.setId(IdWorker.getId());
            noticeMapper.insert(entity);
        } else {
            noticeMapper.updateById(entity);
        }
        return entity;
    }

    public NoticeEntity getNotice(Long noticeId) {
        return noticeMapper.selectById(noticeId);
    }

    /**
     * 管理端：分页查询公告（平台看全部；租户侧看本租户 + 平台 tenantId=0 的系统更新）
     */
    public Page<NoticeEntity> pageNotices(int pageNum, int pageSize, long tenantId, boolean systemUser) {
        LambdaQueryWrapper<NoticeEntity> qw = new LambdaQueryWrapper<>();
        qw.and(w -> w.isNull(NoticeEntity::getDeleted).or().eq(NoticeEntity::getDeleted, 0));
        if (!systemUser) {
            qw.and(w -> w.eq(NoticeEntity::getTenantId, tenantId)
                    .or(w2 -> w2.eq(NoticeEntity::getNoticeType, "SYS_UPDATE").eq(NoticeEntity::getTenantId, 0L)));
        }
        qw.orderByDesc(NoticeEntity::getId);
        return noticeMapper.selectPage(new Page<>(pageNum, pageSize), qw);
    }

    public Page<MessageResponseVo.NoticeMyItem> myNotices(int pageNum, int pageSize, Long userId, Integer readStatus) {
        Long total = noticeUserMapper.countVisibleForUser(userId, readStatus);
        long t = total == null ? 0L : total;
        if (t == 0) {
            return new Page<>(pageNum, pageSize, 0);
        }
        long offset = (pageNum - 1L) * pageSize;
        List<NoticeUserEntity> nuList = noticeUserMapper.selectVisiblePage(userId, readStatus, offset, pageSize);
        if (nuList.isEmpty()) {
            Page<MessageResponseVo.NoticeMyItem> empty = new Page<>(pageNum, pageSize, t);
            empty.setRecords(List.of());
            return empty;
        }
        Set<Long> noticeIds = new HashSet<>();
        for (NoticeUserEntity nu : nuList) {
            noticeIds.add(nu.getNoticeId());
        }
        List<NoticeEntity> notices = noticeMapper.selectList(new LambdaQueryWrapper<NoticeEntity>().in(NoticeEntity::getId, noticeIds));
        Map<Long, NoticeEntity> byId = new HashMap<>();
        for (NoticeEntity n : notices) {
            byId.put(n.getId(), n);
        }
        List<MessageResponseVo.NoticeMyItem> rows = new ArrayList<>();
        for (NoticeUserEntity nu : nuList) {
            NoticeEntity n = byId.get(nu.getNoticeId());
            if (n == null) {
                continue;
            }
            rows.add(new MessageResponseVo.NoticeMyItem(
                    nu.getNoticeId(),
                    n.getTitle(),
                    n.getNoticeType(),
                    nu.getReadStatus(),
                    nu.getTenantId()
            ));
        }
        Page<MessageResponseVo.NoticeMyItem> result = new Page<>(pageNum, pageSize, t);
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

    /**
     * 已发布公告：按范围解析目标用户并写入 msg_notice_user（幂等，不重复插入）
     */
    @Transactional
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
        for (Long uid : userIds) {
            Long existing = noticeUserMapper.selectCount(new LambdaQueryWrapper<NoticeUserEntity>()
                    .eq(NoticeUserEntity::getNoticeId, noticeId)
                    .eq(NoticeUserEntity::getUserId, uid));
            if (existing != null && existing > 0) {
                continue;
            }
            UserEntity u = userMapper.selectById(uid);
            NoticeUserEntity nu = new NoticeUserEntity();
            nu.setId(IdWorker.getId());
            nu.setTenantId(u != null ? u.getTenantId() : notice.getTenantId());
            nu.setNoticeId(noticeId);
            nu.setUserId(uid);
            nu.setReadStatus(0);
            noticeUserMapper.insert(nu);
        }
    }

    /**
     * 扫描待发布且已到点的公告，执行发布并投递
     */
    @Transactional
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
}
