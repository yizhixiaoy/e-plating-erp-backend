CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGINT PRIMARY KEY COMMENT '主键',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_user_role (tenant_id, user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_menu (
  id BIGINT PRIMARY KEY COMMENT '主键',
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父级ID',
  menu_name VARCHAR(64) NOT NULL COMMENT '菜单名称',
  menu_type CHAR(1) NOT NULL COMMENT 'M目录 C菜单 F按钮',
  path VARCHAR(200) NULL COMMENT '路由地址',
  perms VARCHAR(100) NULL COMMENT '权限标识',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台菜单表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
  id BIGINT PRIMARY KEY COMMENT '主键',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  menu_id BIGINT NOT NULL COMMENT '菜单ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_role_menu (tenant_id, role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单关联表';

INSERT INTO sys_role (id, tenant_id, role_name, role_key, data_scope, status, deleted)
VALUES (40001, 20001, '租户管理员', 'TENANT_ADMIN', 1, 0, 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), role_key = VALUES(role_key);

INSERT INTO sys_user_role (id, tenant_id, user_id, role_id)
VALUES (50001, 20001, 10001, 40001)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, perms, status) VALUES
(60001, 0, '租户查看', 'F', '/tenants', 'tenant:view', 0),
(60002, 0, '租户新增', 'F', '/tenants', 'tenant:add', 0),
(60003, 0, '租户编辑', 'F', '/tenants', 'tenant:edit', 0),
(60004, 0, '租户状态', 'F', '/tenants', 'tenant:status', 0),
(60005, 0, '用户查看', 'F', '/users', 'user:view', 0),
(60006, 0, '用户新增', 'F', '/users', 'user:add', 0),
(60007, 0, '用户编辑', 'F', '/users', 'user:edit', 0),
(60008, 0, '用户状态', 'F', '/users', 'user:status', 0),
(60009, 0, '用户重置', 'F', '/users', 'user:reset', 0),
(60010, 0, '角色查看', 'F', '/roles', 'role:view', 0),
(60011, 0, '角色新增', 'F', '/roles', 'role:add', 0),
(60012, 0, '角色编辑', 'F', '/roles', 'role:edit', 0),
(60013, 0, '角色授权', 'F', '/roles', 'role:grant', 0),
(60014, 0, '角色删除', 'F', '/roles', 'role:delete', 0),
(60015, 0, '菜单查看', 'F', '/menus', 'menu:view', 0),
(60016, 0, '菜单新增', 'F', '/menus', 'menu:add', 0),
(60017, 0, '菜单编辑', 'F', '/menus', 'menu:edit', 0),
(60018, 0, '菜单删除', 'F', '/menus', 'menu:delete', 0),
(60019, 0, '日志查看', 'F', '/logs', 'log:view', 0),
(60020, 0, '日志导出', 'F', '/logs', 'log:export', 0),
(60021, 0, '消息新增', 'F', '/messages', 'message:add', 0),
(60022, 0, '消息发布', 'F', '/messages', 'message:publish', 0),
(60023, 0, '消息撤回', 'F', '/messages', 'message:revoke', 0),
(60024, 0, '邮件记录查看', 'F', '/messages', 'message:email:view', 0),
(60025, 0, '小程序记录查看', 'F', '/messages', 'message:mp:view', 0)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), perms = VALUES(perms);

INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id)
SELECT 70000 + m.id, 20001, 40001, m.id
FROM sys_menu m
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id);
