package com.plating.erp.message.controller;

import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.message.entity.NoticeEntity;
import com.plating.erp.message.service.MessageService;
import com.plating.erp.message.vo.MessageResponseVo;
import com.plating.erp.message.vo.MessageVo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {
    private static final Set<Integer> EDITABLE_STATUS = Set.of(0, 1, 3);

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/notices")
    @PreAuthorize("@authz.hasPerm('message:add')")
    public ApiResponse<?> listManageNotices(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        var u = SecurityUtils.currentUser();
        var page = messageService.pageNotices(pageNum, pageSize, u.tenantId(), u.isSystem());
        return ApiResponse.ok(new PageResult<>(page.records(), page.total()));
    }

    @PostMapping("/notices")
    @PreAuthorize("@authz.hasPerm('message:add')")
    @AuditLog(module = "消息中心", operateType = "CREATE", bizModule = "notice", fieldName = "title")
    public ApiResponse<NoticeEntity> createNotice(@Valid @RequestBody MessageVo.NoticeCreateReq body) {
        Long userId = SecurityUtils.currentUser().userId();
        var me = SecurityUtils.currentUser();
        NoticeEntity entity = new NoticeEntity();
        long tid = body.tenantId() == null ? 1L : body.tenantId();
        entity.setTenantId(me.isSystem() ? tid : me.tenantId());
        entity.setNoticeType(body.noticeType() == null ? "INTERNAL_NOTICE" : body.noticeType());
        entity.setTitle(body.title() == null ? "公告" : body.title());
        entity.setContent(body.content() == null ? "" : body.content());
        entity.setLevel(body.level() == null ? 1 : body.level());
        entity.setPublishScope(body.publishScope() == null ? "TENANT" : body.publishScope());
        entity.setTargetJson(body.targetJson() == null ? "{}" : body.targetJson());
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        if (body.scheduledPublishAt() != null) {
            if (!body.scheduledPublishAt().isAfter(LocalDateTime.now())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "预约发布时间必须晚于当前时间");
            }
            entity.setStatus(1);
            entity.setScheduledPublishAt(body.scheduledPublishAt());
        } else {
            entity.setStatus(0);
        }
        return ApiResponse.ok(messageService.saveNotice(entity, userId));
    }

    @PutMapping("/notices/{noticeId}")
    @PreAuthorize("@authz.hasPerm('message:add')")
    @AuditLog(module = "消息中心", operateType = "UPDATE", bizModule = "notice", fieldName = "title")
    public ApiResponse<NoticeEntity> updateNotice(@PathVariable Long noticeId, @Valid @RequestBody MessageVo.NoticeCreateReq body) {
        NoticeEntity found = requireNotice(noticeId);
        assertNoticeAccess(found);
        if (!EDITABLE_STATUS.contains(found.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "当前状态不允许编辑");
        }
        Long userId = SecurityUtils.currentUser().userId();
        found.setNoticeType(body.noticeType() == null ? found.getNoticeType() : body.noticeType());
        found.setTitle(body.title() == null ? found.getTitle() : body.title());
        found.setContent(body.content() == null ? found.getContent() : body.content());
        found.setLevel(body.level() == null ? found.getLevel() : body.level());
        found.setPublishScope(body.publishScope() == null ? found.getPublishScope() : body.publishScope());
        found.setTargetJson(body.targetJson() == null ? found.getTargetJson() : body.targetJson());
        found.setUpdatedBy(userId);
        if (body.scheduledPublishAt() != null) {
            if (!body.scheduledPublishAt().isAfter(LocalDateTime.now())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "预约发布时间必须晚于当前时间");
            }
            found.setScheduledPublishAt(body.scheduledPublishAt());
            found.setStatus(1);
        }
        return ApiResponse.ok(messageService.saveNotice(found, userId));
    }

    @GetMapping("/notices/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> myNotices(@RequestParam(defaultValue = "1") Integer pageNum,
                                    @RequestParam(defaultValue = "20") Integer pageSize,
                                    @RequestParam(required = false) Integer readStatus) {
        var page = messageService.myNotices(pageNum, pageSize, SecurityUtils.currentUser().userId(), readStatus);
        return ApiResponse.ok(new PageResult<>(page.records(), page.total()));
    }

    @PostMapping("/notices/{noticeId}/publish")
    @PreAuthorize("@authz.hasPerm('message:publish')")
    @AuditLog(module = "消息中心", operateType = "PUBLISH", bizModule = "notice", fieldName = "status")
    public ApiResponse<NoticeEntity> publish(@PathVariable Long noticeId) {
        NoticeEntity found = requireNotice(noticeId);
        assertNoticeAccess(found);
        if (found.getStatus() != null && found.getStatus() == 2) {
            throw new BizException(ErrorCode.BAD_REQUEST, "已发布");
        }
        if (found.getStatus() != null && found.getStatus() == 4) {
            throw new BizException(ErrorCode.BAD_REQUEST, "已撤回不可发布");
        }
        Long userId = SecurityUtils.currentUser().userId();
        found.setStatus(2);
        found.setPublishTime(LocalDateTime.now());
        found.setPublishedBy(userId);
        found.setOfflineAt(null);
        found.setScheduledPublishAt(null);
        found.setUpdatedBy(userId);
        NoticeEntity saved = messageService.saveNotice(found, userId);
        messageService.deliverNoticeRecipients(saved.getId());
        return ApiResponse.ok(saved);
    }

    @PostMapping("/notices/{noticeId}/schedule")
    @PreAuthorize("@authz.hasPerm('message:publish')")
    @AuditLog(module = "消息中心", operateType = "SCHEDULE", bizModule = "notice", fieldName = "scheduled_publish_at")
    public ApiResponse<NoticeEntity> schedule(@PathVariable Long noticeId, @Valid @RequestBody MessageVo.NoticeScheduleReq body) {
        NoticeEntity found = requireNotice(noticeId);
        assertNoticeAccess(found);
        if (found.getStatus() != null && found.getStatus() == 2) {
            throw new BizException(ErrorCode.BAD_REQUEST, "已发布请使用下架后再预约");
        }
        if (found.getStatus() != null && found.getStatus() == 4) {
            throw new BizException(ErrorCode.BAD_REQUEST, "已撤回不可预约");
        }
        Long userId = SecurityUtils.currentUser().userId();
        found.setStatus(1);
        found.setScheduledPublishAt(body.scheduledPublishAt());
        found.setUpdatedBy(userId);
        return ApiResponse.ok(messageService.saveNotice(found, userId));
    }

    @PostMapping("/notices/{noticeId}/offline")
    @PreAuthorize("@authz.hasPerm('message:publish')")
    @AuditLog(module = "消息中心", operateType = "OFFLINE", bizModule = "notice", fieldName = "status")
    public ApiResponse<NoticeEntity> offline(@PathVariable Long noticeId) {
        NoticeEntity found = requireNotice(noticeId);
        assertNoticeAccess(found);
        if (found.getStatus() == null || found.getStatus() != 2) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仅已发布内容可下架");
        }
        Long userId = SecurityUtils.currentUser().userId();
        found.setStatus(3);
        found.setOfflineAt(LocalDateTime.now());
        found.setUpdatedBy(userId);
        return ApiResponse.ok(messageService.saveNotice(found, userId));
    }

    @PostMapping("/notices/{noticeId}/online")
    @PreAuthorize("@authz.hasPerm('message:publish')")
    @AuditLog(module = "消息中心", operateType = "ONLINE", bizModule = "notice", fieldName = "status")
    public ApiResponse<NoticeEntity> online(@PathVariable Long noticeId) {
        NoticeEntity found = requireNotice(noticeId);
        assertNoticeAccess(found);
        if (found.getStatus() == null || found.getStatus() != 3) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仅已下架内容可再上线");
        }
        Long userId = SecurityUtils.currentUser().userId();
        found.setStatus(2);
        found.setOfflineAt(null);
        found.setUpdatedBy(userId);
        NoticeEntity saved = messageService.saveNotice(found, userId);
        messageService.deliverNoticeRecipients(saved.getId());
        return ApiResponse.ok(saved);
    }

    @PostMapping("/notices/{noticeId}/revoke")
    @PreAuthorize("@authz.hasPerm('message:revoke')")
    @AuditLog(module = "消息中心", operateType = "REVOKE", bizModule = "notice", fieldName = "status")
    public ApiResponse<NoticeEntity> revoke(@PathVariable Long noticeId) {
        NoticeEntity found = requireNotice(noticeId);
        assertNoticeAccess(found);
        Long userId = SecurityUtils.currentUser().userId();
        found.setStatus(4);
        found.setUpdatedBy(userId);
        return ApiResponse.ok(messageService.saveNotice(found, userId));
    }

    @PatchMapping("/notices/{noticeId}/read")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(module = "消息中心", operateType = "READ", bizModule = "notice", fieldName = "read_status")
    public ApiResponse<CommonResponses.ReadStatusResponse> read(@PathVariable Long noticeId) {
        messageService.readNotice(noticeId, SecurityUtils.currentUser().userId());
        return ApiResponse.ok(new CommonResponses.ReadStatusResponse(noticeId, 1));
    }

    @GetMapping("/emails")
    @PreAuthorize("@authz.hasPerm('message:email:view')")
    public ApiResponse<?> emailRecords(@RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "20") Integer pageSize,
                                       @RequestParam(required = false) Integer sendStatus) {
        var page = messageService.emails(pageNum, pageSize, sendStatus);
        return ApiResponse.ok(new PageResult<>(page.getRecords(), page.getTotal()));
    }

    @GetMapping("/mp-records")
    @PreAuthorize("@authz.hasPerm('message:mp:view')")
    public ApiResponse<List<MessageResponseVo.MpRecordResponse>> mpRecords() {
        return ApiResponse.ok(List.of(new MessageResponseVo.MpRecordResponse(1L, "TMP_001", 1)));
    }

    private NoticeEntity requireNotice(Long noticeId) {
        NoticeEntity found = messageService.getNotice(noticeId);
        if (found == null || (found.getDeleted() != null && found.getDeleted() != 0)) {
            throw new BizException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        return found;
    }

    private void assertNoticeAccess(NoticeEntity found) {
        var u = SecurityUtils.currentUser();
        if (u.isSystem()) {
            return;
        }
        if (found.getTenantId() == null || !found.getTenantId().equals(u.tenantId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该公告");
        }
    }
}
