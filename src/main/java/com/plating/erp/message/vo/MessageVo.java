package com.plating.erp.message.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Map;

public class MessageVo {
    public record NoticeCreateReq(
            Long tenantId,
            @NotBlank(message = "noticeType不能为空") String noticeType,
            @NotBlank(message = "title不能为空") @Size(max = 128, message = "title长度不能超过128") String title,
            @NotBlank(message = "content不能为空") String content,
            @NotNull(message = "level不能为空") @Min(value = 1, message = "level最小为1") @Max(value = 3, message = "level最大为3") Integer level,
            @NotBlank(message = "publishScope不能为空") @Pattern(regexp = "^(PLATFORM|TENANT|DEPT|USER)$", message = "publishScope取值不合法") String publishScope,
            String targetJson,
            Long userId,
            LocalDateTime scheduledPublishAt,
            /** 是否邮件推送:0否 1是 */
            Integer pushEmail
    ) {
    }

    public record NoticeScheduleReq(
            @NotNull(message = "scheduledPublishAt不能为空") @Future(message = "scheduledPublishAt必须晚于当前时间") LocalDateTime scheduledPublishAt
    ) {
    }

    /** 扭平的我的消息视图（包含 readStatus / noticeType） */
    public record MyNoticeView(
            Long noticeId,
            Long noticeUserId,
            Long tenantId,
            String noticeType,
            String title,
            String content,
            Integer level,
            String publishScope,
            LocalDateTime publishTime,
            LocalDateTime createdAt,
            Integer readStatus,
            LocalDateTime readTime
    ) {
    }

    /** 消息中心统计 */
    public record MyNoticeStats(
            long total,
            long unread,
            long todayNew,
            Map<String, Long> unreadByType
    ) {
    }
}
