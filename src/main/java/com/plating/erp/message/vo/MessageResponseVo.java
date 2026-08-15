package com.plating.erp.message.vo;

public class MessageResponseVo {
    public record NoticeMyItem(Long noticeId, String title, String noticeType, Integer readStatus, Long tenantId) {
    }

    public record MpRecordResponse(Long id, String templateId, Integer sendStatus) {
    }
}
