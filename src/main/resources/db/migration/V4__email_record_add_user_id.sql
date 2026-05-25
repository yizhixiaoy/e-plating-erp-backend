-- V4: msg_email_record 增加 receiver_user_id 和 sender_email 字段
ALTER TABLE `msg_email_record` ADD COLUMN `receiver_user_id` bigint NULL DEFAULT NULL COMMENT '接收人用户ID' AFTER `tenant_id`;
ALTER TABLE `msg_email_record` ADD COLUMN `sender_email` varchar(100) NULL DEFAULT NULL COMMENT '发件人邮箱' AFTER `notice_id`;
