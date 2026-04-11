-- 播报/公告：预约发布、上下架、创建修改发布审计字段；接收表补充 updated_at

ALTER TABLE msg_notice
  ADD COLUMN scheduled_publish_at DATETIME NULL COMMENT '预约发布时间' AFTER status,
  ADD COLUMN published_by BIGINT NULL COMMENT '执行发布的操作人' AFTER publish_time,
  ADD COLUMN offline_at DATETIME NULL COMMENT '下架时间' AFTER published_by,
  ADD COLUMN created_by BIGINT NULL COMMENT '创建人' AFTER offline_at,
  ADD COLUMN updated_by BIGINT NULL COMMENT '修改人' AFTER created_at,
  ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间' AFTER updated_by,
  ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否 1是' AFTER updated_at;

ALTER TABLE msg_notice
  MODIFY COLUMN status TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1待发布(预约) 2已发布 3已下架 4已撤回';

ALTER TABLE msg_notice
  ADD KEY idx_tenant_status_schedule (tenant_id, status, scheduled_publish_at);

ALTER TABLE msg_notice_user
  ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '行更新时间' AFTER created_at;
