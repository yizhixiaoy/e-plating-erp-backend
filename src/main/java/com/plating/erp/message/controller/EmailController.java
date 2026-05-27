package com.plating.erp.message.controller;

import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.mapper.UserMapper;
import com.plating.erp.message.entity.EmailRecordEntity;
import com.plating.erp.message.mapper.EmailRecordMapper;
import com.plating.erp.message.service.MessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 邮件管理控制器（独立菜单页）
 */
@RestController
@RequestMapping("/api/v1/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailRecordMapper emailRecordMapper;
    private final MessageService messageService;
    private final UserMapper userMapper;

    /** 分页查询邮件列表 */
    @GetMapping
    @PreAuthorize("@authz.hasPerm('message:email:view')")
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "20") Integer pageSize,
                               @RequestParam(required = false) Integer sendStatus,
                               @RequestParam(required = false) Long operatorId) {
        var page = messageService.emails(pageNum, pageSize, sendStatus, operatorId);
        List<EmailRecordEntity> records = page.getRecords();
        // 批量解析操作人姓名
        Map<Long, String> operatorNames = resolveOperatorNames(records);
        List<Map<String, Object>> enriched = records.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("tenantId", r.getTenantId());
            m.put("receiverUserId", r.getReceiverUserId());
            m.put("noticeId", r.getNoticeId());
            m.put("senderEmail", r.getSenderEmail());
            m.put("receiverEmail", r.getReceiverEmail());
            m.put("subject", r.getSubject());
            m.put("content", r.getContent());
            m.put("sendStatus", r.getSendStatus());
            m.put("failReason", r.getFailReason());
            m.put("retryCount", r.getRetryCount());
            m.put("operatorId", r.getOperatorId());
            m.put("operatorName", operatorNames.get(r.getOperatorId()));
            m.put("sentTime", r.getSentTime());
            m.put("createdAt", r.getCreatedAt());
            m.put("updatedAt", r.getUpdatedAt());
            return m;
        }).collect(Collectors.toList());
        return ApiResponse.ok(new PageResult<>(enriched, page.getTotal()));
    }

    private Map<Long, String> resolveOperatorNames(List<EmailRecordEntity> records) {
        Set<Long> ids = records.stream()
                .map(EmailRecordEntity::getOperatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Collections.emptyMap();
        List<UserEntity> users = userMapper.selectBatchIds(ids);
        return users.stream().collect(Collectors.toMap(
                UserEntity::getId,
                u -> u.getRealName() != null ? u.getRealName() : u.getUsername(),
                (a, b) -> a
        ));
    }

    /** 查看邮件详情 */
    @GetMapping("/{id}")
    @PreAuthorize("@authz.hasPerm('message:email:view')")
    public ApiResponse<EmailRecordEntity> detail(@PathVariable Long id) {
        EmailRecordEntity email = emailRecordMapper.selectById(id);
        if (email == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "邮件记录不存在");
        }
        return ApiResponse.ok(email);
    }

    /** 新建邮件（待发送） */
    @PostMapping
    @PreAuthorize("@authz.hasPerm('message:email:add')")
    @AuditLog(module = "邮件管理", operateType = "CREATE", bizModule = "email")
    public ApiResponse<EmailRecordEntity> create(@Valid @RequestBody EmailCreateRequest req) {
        EmailRecordEntity entity = new EmailRecordEntity();
        entity.setTenantId(SecurityUtils.currentUser().tenantId());
        entity.setReceiverEmail(req.receiverEmail());
        entity.setSubject(req.subject());
        entity.setContent(req.content());
        entity.setSendStatus(0); // 待发送
        entity.setRetryCount(0);
        emailRecordMapper.insert(entity);
        return ApiResponse.ok(entity);
    }

    /** 修改邮件 */
    @PutMapping("/{id}")
    @PreAuthorize("@authz.hasPerm('message:email:edit')")
    @AuditLog(module = "邮件管理", operateType = "UPDATE", bizModule = "email")
    public ApiResponse<EmailRecordEntity> update(@PathVariable Long id,
                                                  @Valid @RequestBody EmailCreateRequest req) {
        EmailRecordEntity email = emailRecordMapper.selectById(id);
        if (email == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "邮件记录不存在");
        }
        if (email.getSendStatus() != 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只能编辑待发送的邮件");
        }
        email.setReceiverEmail(req.receiverEmail());
        email.setSubject(req.subject());
        email.setContent(req.content());
        email.setUpdatedAt(LocalDateTime.now());
        emailRecordMapper.updateById(email);
        return ApiResponse.ok(email);
    }

    /** 发送邮件（将待发送改为已发送） */
    @PostMapping("/{id}/send")
    @PreAuthorize("@authz.hasPerm('message:email:send')")
    @AuditLog(module = "邮件管理", operateType = "SEND", bizModule = "email")
    public ApiResponse<?> send(@PathVariable Long id) {
        EmailRecordEntity email = emailRecordMapper.selectById(id);
        if (email == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "邮件记录不存在");
        }
        if (email.getSendStatus() != 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只能发送待发送状态的邮件");
        }
        // TODO: 接入实际邮件发送服务，目前仅标记状态
        email.setSendStatus(1);
        email.setSentTime(LocalDateTime.now());
        email.setUpdatedAt(LocalDateTime.now());
        emailRecordMapper.updateById(email);
        return ApiResponse.ok("发送成功");
    }

    /** 重试发送失败的邮件 */
    @PostMapping("/{id}/retry")
    @PreAuthorize("@authz.hasPerm('message:email:send')")
    @AuditLog(module = "邮件管理", operateType = "RETRY", bizModule = "email")
    public ApiResponse<?> retry(@PathVariable Long id) {
        EmailRecordEntity email = emailRecordMapper.selectById(id);
        if (email == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "邮件记录不存在");
        }
        if (email.getSendStatus() != 2) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只能重试发送失败的邮件");
        }
        // TODO: 接入实际邮件发送服务
        email.setSendStatus(1);
        email.setSentTime(LocalDateTime.now());
        email.setRetryCount(email.getRetryCount() + 1);
        email.setFailReason(null);
        email.setUpdatedAt(LocalDateTime.now());
        emailRecordMapper.updateById(email);
        return ApiResponse.ok("重试发送成功");
    }

    /** 删除邮件 */
    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.hasPerm('message:email:delete')")
    @AuditLog(module = "邮件管理", operateType = "DELETE", bizModule = "email")
    public ApiResponse<?> delete(@PathVariable Long id) {
        EmailRecordEntity email = emailRecordMapper.selectById(id);
        if (email == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "邮件记录不存在");
        }
        emailRecordMapper.deleteById(id);
        return ApiResponse.ok("删除成功");
    }

    /** 新建/编辑邮件请求体 */
    public record EmailCreateRequest(
            @NotBlank(message = "收件邮箱不能为空") String receiverEmail,
            @NotBlank(message = "邮件主题不能为空") String subject,
            String content
    ) {}
}
