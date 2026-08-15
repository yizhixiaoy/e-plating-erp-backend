package com.plating.erp.message.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.mapper.UserMapper;
import com.plating.erp.message.entity.SmsRecordEntity;
import com.plating.erp.message.mapper.SmsRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 短信记录管理控制器
 */
@RestController
@RequestMapping("/api/v1/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsRecordMapper smsRecordMapper;
    private final UserMapper userMapper;

    /** 分页查询短信记录列表 */
    @GetMapping
    @PreAuthorize("@authz.hasPerm('message:email:view')")
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "20") Integer pageSize,
                               @RequestParam(required = false) Integer sendStatus,
                               @RequestParam(required = false) Long operatorId,
                               @RequestParam(required = false) String smsType) {
        LambdaQueryWrapper<SmsRecordEntity> wrapper = new LambdaQueryWrapper<>();
        if (sendStatus != null) {
            wrapper.eq(SmsRecordEntity::getSendStatus, sendStatus);
        }
        if (operatorId != null) {
            wrapper.eq(SmsRecordEntity::getOperatorId, operatorId);
        }
        if (smsType != null && !smsType.isBlank()) {
            wrapper.eq(SmsRecordEntity::getSmsType, smsType);
        }
        wrapper.orderByDesc(SmsRecordEntity::getId);
        Page<SmsRecordEntity> page = smsRecordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<SmsRecordEntity> records = page.getRecords();

        // 批量解析操作人姓名
        Map<Long, String> operatorNames = resolveOperatorNames(records);
        List<Map<String, Object>> enriched = records.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("tenantId", r.getTenantId());
            m.put("receiverUserId", r.getReceiverUserId());
            m.put("receiverPhone", r.getReceiverPhone());
            m.put("smsType", r.getSmsType());
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

    private Map<Long, String> resolveOperatorNames(List<SmsRecordEntity> records) {
        Set<Long> ids = records.stream()
                .map(SmsRecordEntity::getOperatorId)
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
}
