package com.plating.erp.message;

import com.plating.erp.message.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NoticeScheduledTask {
    private static final Logger log = LoggerFactory.getLogger(NoticeScheduledTask.class);

    private final MessageService messageService;

    public NoticeScheduledTask(MessageService messageService) {
        this.messageService = messageService;
    }

    /** 每分钟检查预约发布的公告 */
    @Scheduled(fixedDelayString = "${app.message.publish-check-ms:60000}")
    public void publishDueNotices() {
        try {
            int n = messageService.publishDueScheduledNotices();
            if (n > 0) {
                log.info("scheduled notice publish: {} notice(s)", n);
            }
        } catch (Exception e) {
            log.error("scheduled notice publish failed", e);
        }
    }
}
