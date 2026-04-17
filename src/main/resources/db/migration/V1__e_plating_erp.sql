
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for auth_scan_session
-- ----------------------------
DROP TABLE IF EXISTS `auth_scan_session`;
CREATE TABLE `auth_scan_session`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `qr_token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '扫码票据',
  `web_client_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '发起端标识',
  `scanner_user_id` bigint NULL DEFAULT NULL COMMENT '扫码确认用户ID',
  `session_status` tinyint NOT NULL DEFAULT 0 COMMENT '0待扫码 1待确认 2已确认 3已过期 4已取消',
  `expire_at` datetime NOT NULL COMMENT '过期时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `confirmed_at` datetime NULL DEFAULT NULL COMMENT '确认时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_qr_token`(`qr_token`) USING BTREE,
  INDEX `idx_tenant_status`(`tenant_id`, `session_status`) USING BTREE,
  INDEX `idx_expire_at`(`expire_at`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '扫码登录会话表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of auth_scan_session
-- ----------------------------

-- ----------------------------
-- Table structure for auth_verify_code
-- ----------------------------
DROP TABLE IF EXISTS `auth_verify_code`;
CREATE TABLE `auth_verify_code`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `channel` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道:SMS/EMAIL',
  `receiver` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号或邮箱',
  `verify_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '验证码',
  `biz_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务类型:LOGIN/RESET_PASSWORD/BIND',
  `expire_at` datetime NOT NULL COMMENT '过期时间',
  `used_status` tinyint NOT NULL DEFAULT 0 COMMENT '0未使用 1已使用',
  `fail_count` int NOT NULL DEFAULT 0 COMMENT '失败次数',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `used_at` datetime NULL DEFAULT NULL COMMENT '使用时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_receiver`(`tenant_id`, `receiver`) USING BTREE,
  INDEX `idx_expire_at`(`expire_at`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '登录验证码记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of auth_verify_code
-- ----------------------------

-- ----------------------------
-- Table structure for base_dict_item
-- ----------------------------
DROP TABLE IF EXISTS `base_dict_item`;
CREATE TABLE `base_dict_item`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典类型',
  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '键值',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态:0正常 1停用',
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_type_value`(`tenant_id`, `dict_type`, `dict_value`) USING BTREE,
  INDEX `idx_tenant_type`(`tenant_id`, `dict_type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '数据字典项表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of base_dict_item
-- ----------------------------
INSERT INTO `base_dict_item` VALUES (1, 0, 'sys_notice_type', '系统更新', 'SYS_UPDATE', 1, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (2, 0, 'sys_notice_type', '内部通知', 'INTERNAL_NOTICE', 2, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (3, 0, 'sys_notice_level', '普通', '1', 1, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (4, 0, 'sys_notice_level', '重要', '2', 2, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (5, 0, 'sys_notice_level', '紧急', '3', 3, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (6, 0, 'sys_notice_status', '草稿', '0', 1, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (7, 0, 'sys_notice_status', '待发布', '1', 2, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (8, 0, 'sys_notice_status', '已发布', '2', 3, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (9, 0, 'sys_notice_status', '已下架', '3', 4, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (10, 0, 'sys_notice_status', '已撤回', '4', 5, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (11, 0, 'sys_data_scope', '全部', '1', 1, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (12, 0, 'sys_data_scope', '本部门及以下', '2', 2, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (13, 0, 'sys_data_scope', '本部门', '3', 3, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (14, 0, 'sys_data_scope', '本人', '4', 4, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (15, 0, 'sys_oper_type', '新增', 'CREATE', 1, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (16, 0, 'sys_oper_type', '修改', 'UPDATE', 2, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (17, 0, 'sys_oper_type', '删除', 'DELETE', 3, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (18, 0, 'sys_oper_type', '查询', 'QUERY', 4, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (19, 0, 'sys_oper_type', '导出', 'EXPORT', 5, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (20, 0, 'sys_oper_type', '登录', 'LOGIN', 6, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (21, 0, 'sys_oper_type', '发布', 'PUBLISH', 7, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (22, 0, 'sys_oper_type', '下架', 'OFFLINE', 8, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_item` VALUES (23, 0, 'sys_oper_type', '撤回', 'REVOKE', 9, 0, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);

-- ----------------------------
-- Table structure for base_dict_type
-- ----------------------------
DROP TABLE IF EXISTS `base_dict_type`;
CREATE TABLE `base_dict_type`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典类型',
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典名称',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态:0正常 1停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_dict_type`(`tenant_id`, `dict_type`) USING BTREE,
  INDEX `idx_tenant_status`(`tenant_id`, `status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '数据字典类型表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of base_dict_type
-- ----------------------------
INSERT INTO `base_dict_type` VALUES (1, 0, 'sys_notice_type', '消息类型', 0, NULL, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_type` VALUES (2, 0, 'sys_notice_level', '消息级别', 0, NULL, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_type` VALUES (3, 0, 'sys_notice_status', '播报状态', 0, NULL, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_type` VALUES (4, 0, 'sys_data_scope', '数据范围', 0, NULL, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_type` VALUES (5, 0, 'sys_oper_type', '操作类型', 0, NULL, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);

-- ----------------------------
-- Table structure for msg_email_record
-- ----------------------------
DROP TABLE IF EXISTS `msg_email_record`;
CREATE TABLE `msg_email_record`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `notice_id` bigint NULL DEFAULT NULL COMMENT '关联消息ID',
  `receiver_email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '收件邮箱',
  `subject` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '邮件主题',
  `send_status` tinyint NOT NULL DEFAULT 0 COMMENT '0待发送 1成功 2失败',
  `fail_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `sent_time` datetime NULL DEFAULT NULL COMMENT '发送时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '邮件发送记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of msg_email_record
-- ----------------------------

-- ----------------------------
-- Table structure for msg_notice
-- ----------------------------
DROP TABLE IF EXISTS `msg_notice`;
CREATE TABLE `msg_notice`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `notice_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息类型',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '内容',
  `level` tinyint NOT NULL DEFAULT 1 COMMENT '级别',
  `publish_scope` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '发布范围',
  `target_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '目标范围JSON',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0草稿 1待发布(预约) 2已发布 3已下架 4已撤回',
  `scheduled_publish_at` datetime NULL DEFAULT NULL COMMENT '预约发布时间',
  `publish_time` datetime NULL DEFAULT NULL COMMENT '发布时间',
  `published_by` bigint NULL DEFAULT NULL COMMENT '执行发布的操作人',
  `offline_at` datetime NULL DEFAULT NULL COMMENT '下架时间',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL COMMENT '修改人',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否 1是',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_status_schedule`(`tenant_id`, `status`, `scheduled_publish_at`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '站内消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of msg_notice
-- ----------------------------

-- ----------------------------
-- Table structure for msg_notice_user
-- ----------------------------
DROP TABLE IF EXISTS `msg_notice_user`;
CREATE TABLE `msg_notice_user`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `notice_id` bigint NOT NULL COMMENT '消息ID',
  `user_id` bigint NOT NULL COMMENT '接收人',
  `read_status` tinyint NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  `read_time` datetime NULL DEFAULT NULL COMMENT '已读时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '行更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_notice_user`(`tenant_id`, `notice_id`, `user_id`) USING BTREE,
  INDEX `idx_tenant_user_status`(`tenant_id`, `user_id`, `read_status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '消息接收状态表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of msg_notice_user
-- ----------------------------

-- ----------------------------
-- Table structure for sys_biz_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_biz_log`;
CREATE TABLE `sys_biz_log`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `biz_module` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务模块',
  `biz_id` bigint NOT NULL COMMENT '业务ID',
  `field_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字段名',
  `old_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '旧值',
  `new_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '新值',
  `user_id` bigint NULL DEFAULT NULL COMMENT '操作人ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '操作人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_biz`(`tenant_id`, `biz_module`, `biz_id`) USING BTREE,
  INDEX `idx_tenant_time`(`tenant_id`, `created_at`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '业务变更日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_biz_log
-- ----------------------------

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父部门ID',
  `dept_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门名称',
  `leader_user_id` bigint NULL DEFAULT NULL COMMENT '负责人',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态:0正常 1停用',
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_parent_name`(`tenant_id`, `parent_id`, `dept_name`) USING BTREE,
  INDEX `idx_tenant_parent`(`tenant_id`, `parent_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------

-- ----------------------------
-- Table structure for sys_login_history
-- ----------------------------
DROP TABLE IF EXISTS `sys_login_history`;
CREATE TABLE `sys_login_history`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID（系统管理员为空）',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `login_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录方式:PASSWORD/SMS_CODE/EMAIL_CODE/SCAN_CODE/WECHAT/DINGTALK',
  `login_time` datetime NOT NULL COMMENT '登录时间',
  `login_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '登录IP',
  `device_info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备信息',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '浏览器UA',
  `login_status` tinyint NOT NULL DEFAULT 1 COMMENT '登录状态:1成功 0失败',
  `fail_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_user`(`tenant_id`, `user_id`) USING BTREE,
  INDEX `idx_user_time`(`user_id`, `login_time`) USING BTREE,
  INDEX `idx_login_time`(`login_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户登录历史表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_login_history
-- ----------------------------

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint NOT NULL COMMENT '主键',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父级ID',
  `menu_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单名称',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'M目录 C菜单 F按钮',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '路由地址',
  `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '权限标识',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '修改人',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '平台菜单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (60001, 0, '租户查看', 'F', '/tenants', 'tenant:view', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60002, 0, '租户新增', 'F', '/tenants', 'tenant:add', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60003, 0, '租户编辑', 'F', '/tenants', 'tenant:edit', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60004, 0, '租户状态', 'F', '/tenants', 'tenant:status', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60005, 0, '用户查看', 'F', '/users', 'user:view', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60006, 0, '用户新增', 'F', '/users', 'user:add', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60007, 0, '用户编辑', 'F', '/users', 'user:edit', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60008, 0, '用户状态', 'F', '/users', 'user:status', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60009, 0, '用户重置', 'F', '/users', 'user:reset', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60010, 0, '角色查看', 'F', '/roles', 'role:view', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60011, 0, '角色新增', 'F', '/roles', 'role:add', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60012, 0, '角色编辑', 'F', '/roles', 'role:edit', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60013, 0, '角色授权', 'F', '/roles', 'role:grant', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60014, 0, '角色删除', 'F', '/roles', 'role:delete', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60015, 0, '菜单查看', 'F', '/menus', 'menu:view', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60016, 0, '菜单新增', 'F', '/menus', 'menu:add', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60017, 0, '菜单编辑', 'F', '/menus', 'menu:edit', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60018, 0, '菜单删除', 'F', '/menus', 'menu:delete', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60019, 0, '日志查看', 'F', '/logs', 'log:view', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60020, 0, '日志导出', 'F', '/logs', 'log:export', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60021, 0, '消息新增', 'F', '/messages', 'message:add', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60022, 0, '消息发布', 'F', '/messages', 'message:publish', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60023, 0, '消息撤回', 'F', '/messages', 'message:revoke', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60024, 0, '邮件记录查看', 'F', '/messages', 'message:email:view', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60025, 0, '小程序记录查看', 'F', '/messages', 'message:mp:view', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `module_title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模块标题',
  `operate_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型',
  `user_id` bigint NOT NULL COMMENT '操作人ID',
  `user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人姓名',
  `request_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求URL',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求方法',
  `method_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '方法名',
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '请求参数(脱敏)',
  `response_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '响应结果',
  `status` tinyint NOT NULL COMMENT '状态:0成功 1失败',
  `error_msg` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误信息',
  `execute_time` int NULL DEFAULT NULL COMMENT '执行时长(ms)',
  `ip_address` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'User-Agent',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_time`(`tenant_id`, `created_at`) USING BTREE,
  INDEX `idx_tenant_user_time`(`tenant_id`, `user_id`, `created_at`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_oper_log
-- ----------------------------

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `role_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名',
  `role_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色权限字符',
  `data_scope` tinyint NOT NULL COMMENT '数据范围',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL COMMENT '修改人',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_role_name`(`tenant_id`, `role_name`) USING BTREE,
  UNIQUE INDEX `uk_tenant_role_key`(`tenant_id`, `role_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (40001, 20001, '租户管理员', 'TENANT_ADMIN', 1, 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33', 0);
INSERT INTO `sys_role` VALUES (40002, 20001, '普通员工', 'EMPLOYEE', 5, 0, NULL, '2026-04-17 01:57:35', NULL, '2026-04-17 01:57:35', 0);

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_role_menu`(`tenant_id`, `role_id`, `menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色菜单关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (80001, 20001, 40002, 60005, '2026-04-17 01:57:35');
INSERT INTO `sys_role_menu` VALUES (80002, 20001, 40002, 60010, '2026-04-17 01:57:35');
INSERT INTO `sys_role_menu` VALUES (80003, 20001, 40002, 60015, '2026-04-17 01:57:35');
INSERT INTO `sys_role_menu` VALUES (80004, 20001, 40002, 60019, '2026-04-17 01:57:35');
INSERT INTO `sys_role_menu` VALUES (80005, 20001, 40002, 60024, '2026-04-17 01:57:35');
INSERT INTO `sys_role_menu` VALUES (80006, 20001, 40002, 60025, '2026-04-17 01:57:35');
INSERT INTO `sys_role_menu` VALUES (130001, 20001, 40001, 60001, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130002, 20001, 40001, 60002, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130003, 20001, 40001, 60003, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130004, 20001, 40001, 60004, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130005, 20001, 40001, 60005, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130006, 20001, 40001, 60006, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130007, 20001, 40001, 60007, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130008, 20001, 40001, 60008, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130009, 20001, 40001, 60009, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130010, 20001, 40001, 60010, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130011, 20001, 40001, 60011, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130012, 20001, 40001, 60012, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130013, 20001, 40001, 60013, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130014, 20001, 40001, 60014, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130015, 20001, 40001, 60015, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130016, 20001, 40001, 60016, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130017, 20001, 40001, 60017, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130018, 20001, 40001, 60018, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130019, 20001, 40001, 60019, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130020, 20001, 40001, 60020, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130021, 20001, 40001, 60021, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130022, 20001, 40001, 60022, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130023, 20001, 40001, 60023, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130024, 20001, 40001, 60024, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130025, 20001, 40001, 60025, '2026-04-17 01:57:33');

-- ----------------------------
-- Table structure for sys_tenant
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户名称',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '租户头像URL',
  `logo_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '企业Logo URL',
  `short_code` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户简称',
  `contact_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联系人',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联系电话',
  `expire_time` datetime NOT NULL COMMENT '到期时间',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态:0正常 1冻结',
  `domain` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '绑定域名',
  `welcome_text` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '欢迎语',
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_short_code`(`short_code`) USING BTREE,
  UNIQUE INDEX `uk_phone`(`phone`) USING BTREE,
  INDEX `idx_expire_time`(`expire_time`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_tenant
-- ----------------------------
INSERT INTO `sys_tenant` VALUES (1, '平台租户', NULL, NULL, 'system', '平台管理员', '13800000001', '2099-12-31 00:00:00', 0, NULL, NULL, NULL, '2026-04-17 01:57:29', NULL, '2026-04-17 01:57:29', 0);
INSERT INTO `sys_tenant` VALUES (20001, '演示租户A', NULL, NULL, 'a', '租户管理员', '13800000002', '2099-12-31 00:00:00', 0, NULL, NULL, NULL, '2026-04-17 01:57:30', NULL, '2026-04-17 01:57:30', 0);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '账号',
  `password_hash` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码哈希',
  `real_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '姓名',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户头像URL',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '部门ID',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `user_type` tinyint NOT NULL COMMENT '用户类型:0平台 1租户',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态:0正常 1停用',
  `last_login_at` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后登录IP',
  `login_count` int NOT NULL DEFAULT 0 COMMENT '登录次数',
  `wechat_openid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信OpenID',
  `dingtalk_userid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '钉钉UserID',
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_username`(`tenant_id`, `username`) USING BTREE,
  UNIQUE INDEX `uk_wechat_openid`(`wechat_openid`) USING BTREE,
  UNIQUE INDEX `uk_dingtalk_userid`(`dingtalk_userid`) USING BTREE,
  UNIQUE INDEX `uk_tenant_phone`(`tenant_id`, `phone`) USING BTREE,
  INDEX `idx_tenant_status`(`tenant_id`, `status`) USING BTREE,
  INDEX `idx_tenant_dept`(`tenant_id`, `dept_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 1, 'system', '$2a$10$7cR05yaoxCH8PeyGGEA/C.UTyfsSzsa7Fuy9jdbbVZ/MgVyoOqwq2', '系统管理员', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, NULL, NULL, NULL, '2026-04-17 01:57:30', NULL, '2026-04-17 01:57:35', 0);
INSERT INTO `sys_user` VALUES (10001, 20001, 'a-admin', '$2a$10$rQkIdARwlTsAHH7R4CD0hO8R9WLiM5u4iKQSdBGENC8E0Hr020ibu', '租户管理员', NULL, NULL, NULL, NULL, 1, 0, NULL, NULL, 0, NULL, NULL, NULL, '2026-04-17 01:57:30', NULL, '2026-04-17 01:57:35', 0);
INSERT INTO `sys_user` VALUES (10002, 20001, 'a-user', '$2a$10$vlCTmMXU2zSApw.h3L8I8.qpiGcQ8KXZ.K2hK5mP8r55iplQYjPlO', '张三', NULL, NULL, '13800000003', 'zhangsan@demo.com', 1, 0, NULL, NULL, 0, NULL, NULL, NULL, '2026-04-17 01:57:35', NULL, '2026-04-17 01:57:35', 0);

-- ----------------------------
-- Table structure for sys_user_recent_tenant
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_recent_tenant`;
CREATE TABLE `sys_user_recent_tenant`  (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `last_login_time` datetime NOT NULL COMMENT '最后登录该租户时间',
  `login_count` int NOT NULL DEFAULT 1 COMMENT '登录该租户次数',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_tenant`(`user_id`, `tenant_id`) USING BTREE,
  INDEX `idx_user_time`(`user_id`, `last_login_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户最近登录租户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_recent_tenant
-- ----------------------------

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_user_role`(`tenant_id`, `user_id`, `role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (50001, 20001, 10001, 40001, '2026-04-17 01:57:33');
INSERT INTO `sys_user_role` VALUES (50002, 20001, 10002, 40002, '2026-04-17 01:57:35');

SET FOREIGN_KEY_CHECKS = 1;
