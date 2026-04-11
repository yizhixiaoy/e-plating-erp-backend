CREATE TABLE IF NOT EXISTS msg_notice (
  id BIGINT PRIMARY KEY COMMENT '主键',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  notice_type VARCHAR(30) NOT NULL COMMENT '消息类型',
  title VARCHAR(128) NOT NULL COMMENT '标题',
  content TEXT NOT NULL COMMENT '内容',
  level TINYINT NOT NULL DEFAULT 1 COMMENT '级别',
  publish_scope VARCHAR(30) NOT NULL COMMENT '发布范围',
  target_json TEXT NULL COMMENT '目标范围JSON',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态',
  publish_time DATETIME NULL COMMENT '发布时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内消息表';

CREATE TABLE IF NOT EXISTS msg_email_record (
  id BIGINT PRIMARY KEY COMMENT '主键',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  notice_id BIGINT NULL COMMENT '关联消息ID',
  receiver_email VARCHAR(100) NOT NULL COMMENT '收件邮箱',
  subject VARCHAR(200) NOT NULL COMMENT '邮件主题',
  send_status TINYINT NOT NULL DEFAULT 0 COMMENT '0待发送 1成功 2失败',
  fail_reason VARCHAR(500) NULL COMMENT '失败原因',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  sent_time DATETIME NULL COMMENT '发送时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮件发送记录表';

CREATE TABLE IF NOT EXISTS msg_notice_user (
  id BIGINT PRIMARY KEY COMMENT '主键',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  notice_id BIGINT NOT NULL COMMENT '消息ID',
  user_id BIGINT NOT NULL COMMENT '接收人',
  read_status TINYINT NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  read_time DATETIME NULL COMMENT '已读时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_notice_user (tenant_id, notice_id, user_id),
  KEY idx_tenant_user_status (tenant_id, user_id, read_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息接收状态表';
