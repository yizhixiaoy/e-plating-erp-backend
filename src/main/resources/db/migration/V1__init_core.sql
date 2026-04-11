CREATE TABLE IF NOT EXISTS sys_tenant (
  id BIGINT PRIMARY KEY COMMENT '主键',
  tenant_name VARCHAR(64) NOT NULL COMMENT '租户名称',
  avatar_url VARCHAR(255) NULL COMMENT '租户头像URL',
  short_code VARCHAR(16) NOT NULL COMMENT '租户简称',
  contact_name VARCHAR(32) NOT NULL COMMENT '联系人',
  phone VARCHAR(20) NOT NULL COMMENT '联系电话',
  expire_time DATETIME NOT NULL COMMENT '到期时间',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0正常 1冻结',
  domain VARCHAR(128) NULL COMMENT '绑定域名',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_short_code (short_code),
  UNIQUE KEY uk_phone (phone),
  KEY idx_expire_time (expire_time),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户信息表';

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY COMMENT '主键',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  username VARCHAR(32) NOT NULL COMMENT '账号',
  password_hash VARCHAR(100) NOT NULL COMMENT '密码哈希',
  real_name VARCHAR(30) NOT NULL COMMENT '姓名',
  avatar_url VARCHAR(255) NULL COMMENT '用户头像URL',
  dept_id BIGINT NULL COMMENT '部门ID',
  phone VARCHAR(20) NULL COMMENT '手机号',
  email VARCHAR(100) NULL COMMENT '邮箱',
  user_type TINYINT NOT NULL COMMENT '用户类型:0平台 1租户',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0正常 1停用',
  last_login_at DATETIME NULL COMMENT '最后登录时间',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_username (tenant_id, username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

INSERT INTO sys_tenant (id, tenant_name, short_code, contact_name, phone, expire_time, status, deleted)
VALUES (1, '平台租户', 'system', '平台管理员', '13800000001', '2099-12-31 00:00:00', 0, 0)
ON DUPLICATE KEY UPDATE tenant_name = VALUES(tenant_name);

INSERT INTO sys_user (id, tenant_id, username, password_hash, real_name, user_type, status, deleted)
VALUES (1, 1, 'system', '$2a$10$demo_hash_replace_me', '系统管理员', 0, 0, 0)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name);

INSERT INTO sys_tenant (id, tenant_name, short_code, contact_name, phone, expire_time, status, deleted)
VALUES (20001, '演示租户A', 'a', '租户管理员', '13800000002', '2099-12-31 00:00:00', 0, 0)
ON DUPLICATE KEY UPDATE tenant_name = VALUES(tenant_name);

INSERT INTO sys_user (id, tenant_id, username, password_hash, real_name, user_type, status, deleted)
VALUES (10001, 20001, 'a-admin', '123456', '租户管理员', 1, 0, 0)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name);
