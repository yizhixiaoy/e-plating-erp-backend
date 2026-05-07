/*
 Navicat Premium Data Transfer

 Source Server         : db1
 Source Server Type    : MySQL
 Source Server Version : 80030
 Source Host           : localhost:3306
 Source Schema         : e_plating_erp

 Target Server Type    : MySQL
 Target Server Version : 80030
 File Encoding         : 65001

 Date: 28/04/2026 00:03:48
*/

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '扫码登录会话表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '登录验证码记录表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '数据字典项表' ROW_FORMAT = DYNAMIC;

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
INSERT INTO `base_dict_item` VALUES (24, 0, 'sys_menu_type', '目录', 'M', 1, 0, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (25, 0, 'sys_menu_type', '菜单', 'C', 2, 0, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (26, 0, 'sys_menu_type', '按钮', 'F', 3, 0, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (27, 0, 'sys_status', '正常', '0', 1, 0, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (28, 0, 'sys_status', '停用', '1', 2, 0, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (29, 0, 'sys_yes_no', '否', '0', 1, 0, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (30, 0, 'sys_yes_no', '是', '1', 2, 0, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);

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
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_dict_type`(`tenant_id`, `dict_type`) USING BTREE,
  INDEX `idx_tenant_status`(`tenant_id`, `status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '数据字典类型表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of base_dict_type
-- ----------------------------
INSERT INTO `base_dict_type` VALUES (1, 0, 'sys_notice_type', '消息类型', 0, 0, NULL, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_type` VALUES (2, 0, 'sys_notice_level', '消息级别', 0, 0, NULL, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_type` VALUES (3, 0, 'sys_notice_status', '播报状态', 0, 0, NULL, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_type` VALUES (4, 0, 'sys_data_scope', '数据范围', 0, 0, NULL, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_type` VALUES (5, 0, 'sys_oper_type', '操作类型', 0, 0, NULL, NULL, '2026-04-17 01:57:34', NULL, '2026-04-17 01:57:34', 0);
INSERT INTO `base_dict_type` VALUES (6, 0, 'sys_menu_type', '菜单类型', 0, 0, 'M=目录 C=菜单 F=按钮', NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `base_dict_type` VALUES (7, 0, 'sys_status', '通用状态', 0, 0, '0=正常 1=停用', NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `base_dict_type` VALUES (8, 0, 'sys_yes_no', '是否', 0, 0, '0=否 1=是', NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '邮件发送记录表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '站内消息表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '消息接收状态表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '业务变更日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_biz_log
-- ----------------------------
INSERT INTO `sys_biz_log` VALUES (2046220738990465026, 1, 'user', 0, 'password_hash', '10001', NULL, 1, 'system', '2026-04-20 21:33:35');
INSERT INTO `sys_biz_log` VALUES (2046233520993492994, 1, 'user', 1, 'username', '1', '\"system\"', 1, 'system', '2026-04-20 22:24:23');
INSERT INTO `sys_biz_log` VALUES (2046233563699896322, 1, 'user', 1, 'username', '1', '\"system\"', 1, 'system', '2026-04-20 22:24:33');
INSERT INTO `sys_biz_log` VALUES (2046247626232397825, 1, 'user', 0, 'password_hash', '10001', NULL, 1, 'system', '2026-04-20 23:20:26');
INSERT INTO `sys_biz_log` VALUES (2046247658486595586, 1, 'user', 0, 'password_hash', '10002', NULL, 1, 'system', '2026-04-20 23:20:33');
INSERT INTO `sys_biz_log` VALUES (2046259321713573890, 1, 'tenant', 1, 'tenant_name', '1', NULL, 1, 'system', '2026-04-21 00:06:54');
INSERT INTO `sys_biz_log` VALUES (2046634759690469377, 1, 'tenant', 20001, 'tenant_name', '20001', NULL, 1, 'system', '2026-04-22 00:58:46');
INSERT INTO `sys_biz_log` VALUES (2046635083188748289, 1, 'tenant', 1, 'tenant_name', '1', NULL, 1, 'system', '2026-04-22 01:00:03');
INSERT INTO `sys_biz_log` VALUES (2046635183797518338, 1, 'tenant', 1, 'tenant_name', '1', NULL, 1, 'system', '2026-04-22 01:00:27');
INSERT INTO `sys_biz_log` VALUES (2047332327898599425, 1, 'user', 0, 'avatarUrl', NULL, NULL, 1, 'system', '2026-04-23 23:10:39');
INSERT INTO `sys_biz_log` VALUES (2047351467543744514, 1, 'user', 0, 'avatarUrl', NULL, '\"iam%2Favatar%2F2026_04_24%2Fa45221b5e95b449596cc3f0def0eb55e.jpg\"', 1, 'system', '2026-04-24 00:26:42');
INSERT INTO `sys_biz_log` VALUES (2047713943426768898, 1, 'tenant', 2047713939437985793, 'tenant_name', NULL, NULL, 1, 'system', '2026-04-25 00:27:03');
INSERT INTO `sys_biz_log` VALUES (2047713982387658754, 1, 'tenant', 20001, 'tenant_name', '\"20001\"', NULL, 1, 'system', '2026-04-25 00:27:12');
INSERT INTO `sys_biz_log` VALUES (2047714722363547650, 1, 'tenant', 2047713939437985793, 'tenant_name', '\"2047713939437985793\"', NULL, 1, 'system', '2026-04-25 00:30:09');
INSERT INTO `sys_biz_log` VALUES (2047714908980731906, 1, 'tenant', 0, 'tenant_name', '\"2047713939437985793\"', NULL, 1, 'system', '2026-04-25 00:30:53');
INSERT INTO `sys_biz_log` VALUES (2047715728434487297, 1, 'tenant', 2047713939437985793, 'tenant_name', '\"2047713939437985793\"', NULL, 1, 'system', '2026-04-25 00:34:09');
INSERT INTO `sys_biz_log` VALUES (2047716387401584641, 1, 'tenant', 2047713939437985793, 'tenant_name', '\"2047713939437985793\"', NULL, 1, 'system', '2026-04-25 00:36:46');
INSERT INTO `sys_biz_log` VALUES (2047716717811994625, 1, 'tenant', 2047713939437985793, 'tenant_name', '\"2047713939437985793\"', NULL, 1, 'system', '2026-04-25 00:38:04');
INSERT INTO `sys_biz_log` VALUES (2047719004622135298, 1, 'tenant', 2047713939437985793, 'tenant_name', '\"2047713939437985793\"', NULL, 1, 'system', '2026-04-25 00:47:10');
INSERT INTO `sys_biz_log` VALUES (2047719514766913537, 1, 'tenant', 2047713939437985793, 'tenant_name', '\"2047713939437985793\"', NULL, 1, 'system', '2026-04-25 00:49:11');
INSERT INTO `sys_biz_log` VALUES (2047719789481291777, 1, 'tenant', 2047713939437985793, 'tenant_name', '\"2047713939437985793\"', NULL, 1, 'system', '2026-04-25 00:50:17');
INSERT INTO `sys_biz_log` VALUES (2047730440173907969, 1, 'tenant', 2047713939437985793, 'tenant_name', '\"2047713939437985793\"', NULL, 1, 'system', '2026-04-25 01:32:36');
INSERT INTO `sys_biz_log` VALUES (2047730625679585282, 1, 'tenant', 2047713939437985793, 'tenant_name', '\"2047713939437985793\"', NULL, 1, 'system', '2026-04-25 01:33:20');
INSERT INTO `sys_biz_log` VALUES (2048771282334388225, 1, 'user', 0, 'passwordHash', '\"10001\"', NULL, 1, 'system', '2026-04-27 22:28:32');
INSERT INTO `sys_biz_log` VALUES (2048771354073763842, 1, 'user', 0, 'passwordHash', '\"10002\"', NULL, 1, 'system', '2026-04-27 22:28:49');

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户登录历史表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_login_history
-- ----------------------------
INSERT INTO `sys_login_history` VALUES (2045917521924710402, 1, 1, 'PASSWORD', '2026-04-20 01:28:43', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-20 01:28:43');
INSERT INTO `sys_login_history` VALUES (2045919619286425602, 1, 1, 'PASSWORD', '2026-04-20 01:37:03', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-20 01:37:03');
INSERT INTO `sys_login_history` VALUES (2045921929957859330, 1, 1, 'PASSWORD', '2026-04-20 01:46:14', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-20 01:46:14');
INSERT INTO `sys_login_history` VALUES (2045924849617051649, 1, 1, 'PASSWORD', '2026-04-20 01:57:50', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-20 01:57:50');
INSERT INTO `sys_login_history` VALUES (2045930915599851522, 1, 1, 'PASSWORD', '2026-04-20 02:21:56', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-20 02:21:56');
INSERT INTO `sys_login_history` VALUES (2045931490278219777, 1, 1, 'PASSWORD', '2026-04-20 02:24:14', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-20 02:24:14');
INSERT INTO `sys_login_history` VALUES (2045932845961117697, 1, 1, 'PASSWORD', '2026-04-20 02:29:37', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-20 02:29:37');
INSERT INTO `sys_login_history` VALUES (2046203652939493378, 1, 1, 'PASSWORD', '2026-04-20 20:25:42', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-20 20:25:42');
INSERT INTO `sys_login_history` VALUES (2046215611604819969, 1, 1, 'PASSWORD', '2026-04-20 21:13:13', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-20 21:13:13');
INSERT INTO `sys_login_history` VALUES (2046226675759341570, 1, 1, 'PASSWORD', '2026-04-20 21:57:11', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-20 21:57:11');
INSERT INTO `sys_login_history` VALUES (2046247815542308866, 20001, 10001, 'PASSWORD', '2026-04-20 23:21:11', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-20 23:21:11');
INSERT INTO `sys_login_history` VALUES (2046255238902562817, 20001, 10001, 'PASSWORD', '2026-04-20 23:50:41', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-20 23:50:41');
INSERT INTO `sys_login_history` VALUES (2046256727142285313, 20001, 10001, 'PASSWORD', '2026-04-20 23:56:36', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-20 23:56:36');
INSERT INTO `sys_login_history` VALUES (2046259111025295362, 1, 1, 'PASSWORD', '2026-04-21 00:06:04', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-21 00:06:04');
INSERT INTO `sys_login_history` VALUES (2046259388574973953, 1, 1, 'PASSWORD', '2026-04-21 00:07:11', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-21 00:07:11');
INSERT INTO `sys_login_history` VALUES (2046635392183123970, 1, 1, 'PASSWORD', '2026-04-22 01:01:17', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-22 01:01:17');
INSERT INTO `sys_login_history` VALUES (2046947505220947969, 1, 1, 'PASSWORD', '2026-04-22 21:41:30', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-22 21:41:30');
INSERT INTO `sys_login_history` VALUES (2046948814439387137, 1, 1, 'PASSWORD', '2026-04-22 21:46:42', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-22 21:46:42');
INSERT INTO `sys_login_history` VALUES (2047674203780419586, 1, 1, 'PASSWORD', '2026-04-24 21:49:09', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-24 21:49:09');
INSERT INTO `sys_login_history` VALUES (2047675388734300161, 1, 1, 'PASSWORD', '2026-04-24 21:53:51', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-24 21:53:51');
INSERT INTO `sys_login_history` VALUES (2047684093911588866, 1, 1, 'PASSWORD', '2026-04-24 22:28:27', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-24 22:28:27');
INSERT INTO `sys_login_history` VALUES (2047685810073903105, 1, 1, 'PASSWORD', '2026-04-24 22:35:16', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-24 22:35:16');
INSERT INTO `sys_login_history` VALUES (2047702110363983874, 1, 1, 'PASSWORD', '2026-04-24 23:40:02', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-24 23:40:02');
INSERT INTO `sys_login_history` VALUES (2047705003250950146, 1, 1, 'PASSWORD', '2026-04-24 23:51:32', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-24 23:51:32');
INSERT INTO `sys_login_history` VALUES (2047707110662217730, 1, 1, 'PASSWORD', '2026-04-24 23:59:54', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Qoder/0.14.1 Chrome/138.0.7204.251 Electron/37.7.0 Safari/537.36', 1, NULL, '2026-04-24 23:59:54');
INSERT INTO `sys_login_history` VALUES (2048788981747814402, 1, 1, 'PASSWORD', '2026-04-27 23:38:53', '127.0.0.1', 'Win32', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', 1, NULL, '2026-04-27 23:38:53');

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint NOT NULL COMMENT '主键',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父级ID',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID(0=平台级)',
  `menu_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单名称',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'M目录 C菜单 F按钮',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '路由地址',
  `icon` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '菜单图标',
  `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '权限标识',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序',
  `visible` tinyint NOT NULL DEFAULT 1 COMMENT '是否可见:0隐藏 1显示',
  `component` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '组件路径',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '修改人',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志:0正常 1删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_parent`(`tenant_id`, `parent_id`) USING BTREE,
  INDEX `idx_tenant_status`(`tenant_id`, `status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '平台菜单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, 0, 0, '工作台', 'C', 'workbench', 'HomeFilled', NULL, 0, 0, 1, 'WorkbenchView', NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (10, 0, 0, '系统管理', 'M', 'system', 'Setting', NULL, 0, 1, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (11, 10, 0, '租户管理', 'C', 'tenants', 'OfficeBuilding', NULL, 0, 0, 1, 'TenantView', NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (12, 10, 0, '用户管理', 'C', 'users', 'User', NULL, 0, 1, 1, 'UserView', NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (13, 10, 0, '角色管理', 'C', 'roles', 'Key', NULL, 0, 2, 1, 'RoleView', NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (14, 10, 0, '菜单管理', 'C', 'menus', 'Menu', NULL, 0, 3, 1, 'MenuView', NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (15, 10, 0, '字典管理', 'C', 'dicts', 'Notebook', NULL, 0, 4, 1, 'DictView', NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (20, 0, 0, '运维中心', 'M', 'ops', 'Monitor', NULL, 0, 2, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (21, 20, 0, '日志中心', 'C', 'logs', 'Document', NULL, 0, 0, 1, 'LogView', NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (22, 20, 0, '消息管理', 'C', 'messages', 'Bell', NULL, 0, 1, 1, 'MessageView', NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (30, 0, 0, '个人中心', 'C', 'profile', 'Avatar', NULL, 0, 99, 1, 'ProfileView', NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (100, 1, 0, '查看工作台', 'F', '', '', 'workbench:view', 0, 0, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (110, 11, 0, '租户查看', 'F', '', '', 'tenant:view', 0, 0, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (111, 11, 0, '租户新增', 'F', '', '', 'tenant:add', 0, 1, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (112, 11, 0, '租户编辑', 'F', '', '', 'tenant:edit', 0, 2, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (113, 11, 0, '租户状态', 'F', '', '', 'tenant:status', 0, 3, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (120, 12, 0, '用户查看', 'F', '', '', 'user:view', 0, 0, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (121, 12, 0, '用户新增', 'F', '', '', 'user:add', 0, 1, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (122, 12, 0, '用户编辑', 'F', '', '', 'user:edit', 0, 2, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (123, 12, 0, '用户停用', 'F', '', '', 'user:disable', 0, 3, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (124, 12, 0, '用户启用', 'F', '', '', 'user:enable', 0, 4, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (125, 12, 0, '重置密码', 'F', '', '', 'user:resetPwd', 0, 5, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (126, 12, 0, '查看租户筛选', 'F', '', '', 'user:view:tenant', 0, 6, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (127, 12, 0, '新增用户指定租户', 'F', '', '', 'user:add:tenant', 0, 7, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (130, 13, 0, '角色查看', 'F', '', '', 'role:view', 0, 0, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (131, 13, 0, '角色新增', 'F', '', '', 'role:add', 0, 1, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (132, 13, 0, '角色编辑', 'F', '', '', 'role:edit', 0, 2, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (133, 13, 0, '角色授权', 'F', '', '', 'role:grant', 0, 3, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (134, 13, 0, '角色删除', 'F', '', '', 'role:delete', 0, 4, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (140, 14, 0, '菜单查看', 'F', '', '', 'menu:view', 0, 0, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (141, 14, 0, '菜单新增', 'F', '', '', 'menu:add', 0, 1, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (142, 14, 0, '菜单编辑', 'F', '', '', 'menu:edit', 0, 2, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (143, 14, 0, '菜单删除', 'F', '', '', 'menu:delete', 0, 3, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (150, 15, 0, '字典查看', 'F', '', '', 'dict:view', 0, 0, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (151, 15, 0, '字典新增', 'F', '', '', 'dict:add', 0, 1, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (152, 15, 0, '字典编辑', 'F', '', '', 'dict:edit', 0, 2, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (153, 15, 0, '字典删除', 'F', '', '', 'dict:delete', 0, 3, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (210, 21, 0, '日志查看', 'F', '', '', 'log:view', 0, 0, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (211, 21, 0, '日志导出', 'F', '', '', 'log:export', 0, 1, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (220, 22, 0, '消息查看', 'F', '', '', 'message:view', 0, 0, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (221, 22, 0, '消息新增', 'F', '', '', 'message:add', 0, 1, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (222, 22, 0, '消息发布', 'F', '', '', 'message:publish', 0, 2, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (223, 22, 0, '消息撤回', 'F', '', '', 'message:revoke', 0, 3, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (224, 22, 0, '邮件记录查看', 'F', '', '', 'message:email:view', 0, 4, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (225, 22, 0, '小程序记录查看', 'F', '', '', 'message:mp:view', 0, 5, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (300, 30, 0, '查看个人信息', 'F', '', '', 'profile:view', 0, 0, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (301, 30, 0, '编辑个人信息', 'F', '', '', 'profile:edit', 0, 1, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (302, 30, 0, '修改密码', 'F', '', '', 'profile:password', 0, 2, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_menu` VALUES (303, 30, 0, '编辑公司信息', 'F', '', '', 'company:edit', 0, 3, 1, NULL, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_oper_log
-- ----------------------------
INSERT INTO `sys_oper_log` VALUES (2046220738763972609, 1, '用户管理', 'RESET_PASSWORD', 1, 'system', '/api/v1/users/10001/reset-password', 'PATCH', 'UserController.resetPassword', '[10001]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"userId\":10001,\"newPassword\":\"Init@123456\",\"needChange\":true,\"sessionsInvalidated\":true},\"traceId\":\"trace-1776692015696\",\"timestamp\":1776692015696}', 0, NULL, 224, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-20 21:33:35');
INSERT INTO `sys_oper_log` VALUES (2046233520808943618, 1, '用户管理', 'UPDATE', 1, 'system', '/api/v1/users/1', 'PUT', 'UserController.update', '[1,{\"username\":\"system\",\"realName\":\"张三\",\"avatarUrl\":\"\",\"deptId\":null,\"position\":\"\",\"leaderUserId\":null,\"phone\":\"***\",\"email\":\"\"}]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"id\":1,\"tenantId\":1,\"username\":\"system\",\"passwordHash\":\"$2a$10$7cR05yaoxCH8PeyGGEA/C.UTyfsSzsa7Fuy9jdbbVZ/MgVyoOqwq2\",\"realName\":\"张三\",\"avatarUrl\":null,\"deptId\":null,\"position\":null,\"leaderUserId\":null,\"phone\":null,\"officePhone\":null,\"email\":null,\"joinDate\":null,\"employeeStatus\":0,\"userType\":0,\"status\":0,\"lastLoginAt\":\"2026-04-20T21:57:11\",\"lastLoginIp\":\"127.0.0.1\",\"loginCount\":10,\"wechatOpenid\":null,\"dingtalkUserid\":null,\"createdBy\":null,\"createdAt\":\"2026-04-17T01:57:30\",\"updatedBy\":null,\"updatedAt\":\"2026-04-20T21:57:11\",\"deleted\":0},\"traceId\":\"trace-1776695063197\",\"timestamp\":1776695063197}', 0, NULL, 181, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-20 22:24:23');
INSERT INTO `sys_oper_log` VALUES (2046233563515346945, 1, '用户管理', 'UPDATE', 1, 'system', '/api/v1/users/1', 'PUT', 'UserController.update', '[1,{\"username\":\"system\",\"realName\":\"张坤\",\"avatarUrl\":\"\",\"deptId\":null,\"position\":\"\",\"leaderUserId\":null,\"phone\":\"***\",\"email\":\"\"}]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"id\":1,\"tenantId\":1,\"username\":\"system\",\"passwordHash\":\"$2a$10$7cR05yaoxCH8PeyGGEA/C.UTyfsSzsa7Fuy9jdbbVZ/MgVyoOqwq2\",\"realName\":\"张坤\",\"avatarUrl\":null,\"deptId\":null,\"position\":null,\"leaderUserId\":null,\"phone\":null,\"officePhone\":null,\"email\":null,\"joinDate\":null,\"employeeStatus\":0,\"userType\":0,\"status\":0,\"lastLoginAt\":\"2026-04-20T21:57:11\",\"lastLoginIp\":\"127.0.0.1\",\"loginCount\":10,\"wechatOpenid\":null,\"dingtalkUserid\":null,\"createdBy\":null,\"createdAt\":\"2026-04-17T01:57:30\",\"updatedBy\":null,\"updatedAt\":\"2026-04-20T21:57:11\",\"deleted\":0},\"traceId\":\"trace-1776695073380\",\"timestamp\":1776695073380}', 0, NULL, 168, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-20 22:24:33');
INSERT INTO `sys_oper_log` VALUES (2046247626014294017, 1, '用户管理', 'RESET_PASSWORD', 1, 'system', '/api/v1/users/10001/reset-password', 'PATCH', 'UserController.resetPassword', '[10001]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"userId\":10001,\"newPassword\":\"Init@123456\",\"needChange\":true,\"sessionsInvalidated\":true},\"traceId\":\"trace-1776698426138\",\"timestamp\":1776698426138}', 0, NULL, 273, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-20 23:20:26');
INSERT INTO `sys_oper_log` VALUES (2046247658260103169, 1, '用户管理', 'RESET_PASSWORD', 1, 'system', '/api/v1/users/10002/reset-password', 'PATCH', 'UserController.resetPassword', '[10002]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"userId\":10002,\"newPassword\":\"Init@123456\",\"needChange\":true,\"sessionsInvalidated\":true},\"traceId\":\"trace-1776698433828\",\"timestamp\":1776698433828}', 0, NULL, 227, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-20 23:20:33');
INSERT INTO `sys_oper_log` VALUES (2046259321373835265, 1, '租户管理', 'UPDATE', 1, 'system', '/api/v1/tenants/1', 'PUT', 'TenantController.update', '[1,{\"tenantName\":\"平台租户\",\"logoUrl\":\"\",\"shortCode\":\"system\",\"contactName\":\"平台管理员\",\"phone\":\"***\",\"expireTime\":\"2099-12-31T00:00:00\",\"domain\":\"\",\"welcomeText\":\"{\\n  \\\"welcome\\\": \\\"欢迎{username}使用XX电镀ERP系统\\\",\\n  \\\"features\\\": {\\n    \\\"enableMobile\\\": true,\\n    \\\"enableSSO\\\": false\\n  },\\n  \\\"footerText\\\": \\\"© 2024 XX电镀科技有限公司\\\"\\n}\"}]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"id\":1,\"tenantName\":\"平台租户\",\"logoUrl\":null,\"shortCode\":\"system\",\"contactName\":\"平台管理员\",\"phone\":\"13800000001\",\"expireTime\":\"2099-12-31T00:00:00\",\"status\":0,\"domain\":null,\"welcomeText\":\"{\\n  \\\"welcome\\\": \\\"欢迎{username}使用XX电镀ERP系统\\\",\\n  \\\"features\\\": {\\n    \\\"enableMobile\\\": true,\\n    \\\"enableSSO\\\": false\\n  },\\n  \\\"footerText\\\": \\\"© 2024 XX电镀科技有限公司\\\"\\n}\",\"createdBy\":null,\"createdAt\":\"2026-04-17T01:57:29\",\"updatedBy\":null,\"updatedAt\":\"2026-04-17T01:57:29\",\"deleted\":0},\"traceId\":\"trace-1776701214523\",\"timestamp\":1776701214523}', 0, NULL, 424, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-21 00:06:54');
INSERT INTO `sys_oper_log` VALUES (2046634759447199746, 1, '租户管理', 'UPDATE', 1, 'system', '/api/v1/tenants/20001', 'PUT', 'TenantController.update', '[20001,{\"tenantName\":\"测试公司A\",\"logoUrl\":\"\",\"shortCode\":\"CSGSA\",\"contactName\":\"李四\",\"phone\":\"***\",\"expireTime\":\"2099-12-31T00:00:00\",\"domain\":\"\",\"welcomeText\":\"\"}]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"id\":20001,\"tenantName\":\"测试公司A\",\"logoUrl\":null,\"shortCode\":\"CSGSA\",\"contactName\":\"李四\",\"phone\":\"13800000002\",\"expireTime\":\"2099-12-31T00:00:00\",\"status\":0,\"domain\":null,\"welcomeText\":null,\"createdBy\":null,\"createdAt\":\"2026-04-17T01:57:30\",\"updatedBy\":null,\"updatedAt\":\"2026-04-17T01:57:30\",\"deleted\":0},\"traceId\":\"trace-1776790725909\",\"timestamp\":1776790725909}', 0, NULL, 209, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-22 00:58:45');
INSERT INTO `sys_oper_log` VALUES (2046635082907729922, 1, '租户管理', 'UPDATE', 1, 'system', '/api/v1/tenants/1', 'PUT', 'TenantController.update', '[1,{\"tenantName\":\"智创科技\",\"logoUrl\":\"\",\"shortCode\":\"system\",\"contactName\":\"张坤\",\"phone\":\"***\",\"expireTime\":\"2099-12-31T00:00:00\",\"domain\":\"\",\"welcomeText\":\"{\\n  \\\"welcome\\\": \\\"欢迎{username}使用XX电镀ERP系统\\\",\\n  \\\"features\\\": {\\n    \\\"enableMobile\\\": true,\\n    \\\"enableSSO\\\": false\\n  },\\n  \\\"footerText\\\": \\\"© 2024 XX电镀科技有限公司\\\"\\n}\"}]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"id\":1,\"tenantName\":\"智创科技\",\"logoUrl\":null,\"shortCode\":\"system\",\"contactName\":\"张坤\",\"phone\":\"18375219477\",\"expireTime\":\"2099-12-31T00:00:00\",\"status\":0,\"domain\":null,\"welcomeText\":\"{\\n  \\\"welcome\\\": \\\"欢迎{username}使用XX电镀ERP系统\\\",\\n  \\\"features\\\": {\\n    \\\"enableMobile\\\": true,\\n    \\\"enableSSO\\\": false\\n  },\\n  \\\"footerText\\\": \\\"© 2024 XX电镀科技有限公司\\\"\\n}\",\"createdBy\":null,\"createdAt\":\"2026-04-17T01:57:29\",\"updatedBy\":null,\"updatedAt\":\"2026-04-17T01:57:29\",\"deleted\":0},\"traceId\":\"trace-1776790803063\",\"timestamp\":1776790803063}', 0, NULL, 427, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-22 01:00:03');
INSERT INTO `sys_oper_log` VALUES (2046635183663300610, 1, '租户管理', 'UPDATE', 1, 'system', '/api/v1/tenants/1', 'PUT', 'TenantController.update', '[1,{\"tenantName\":\"智创科技\",\"logoUrl\":\"\",\"shortCode\":\"system\",\"contactName\":\"张坤\",\"phone\":\"***\",\"expireTime\":\"2099-12-31T00:00:00\",\"domain\":\"\",\"welcomeText\":\"{\\n  \\\"welcome\\\": \\\"欢迎{username}使用XX电镀ERP系统\\\",\\n  \\\"features\\\": {\\n    \\\"enableMobile\\\": true,\\n    \\\"enableSSO\\\": false\\n  },\\n  \\\"footerText\\\": \\\"© 2024 XX电镀科技有限公司\\\"\\n}\"}]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"id\":1,\"tenantName\":\"智创科技\",\"logoUrl\":null,\"shortCode\":\"system\",\"contactName\":\"张坤\",\"phone\":\"18375219477\",\"expireTime\":\"2099-12-31T00:00:00\",\"status\":0,\"domain\":null,\"welcomeText\":\"{\\n  \\\"welcome\\\": \\\"欢迎{username}使用XX电镀ERP系统\\\",\\n  \\\"features\\\": {\\n    \\\"enableMobile\\\": true,\\n    \\\"enableSSO\\\": false\\n  },\\n  \\\"footerText\\\": \\\"© 2024 XX电镀科技有限公司\\\"\\n}\",\"createdBy\":null,\"createdAt\":\"2026-04-17T01:57:29\",\"updatedBy\":null,\"updatedAt\":\"2026-04-17T01:57:29\",\"deleted\":0},\"traceId\":\"trace-1776790827085\",\"timestamp\":1776790827085}', 0, NULL, 7, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-22 01:00:27');
INSERT INTO `sys_oper_log` VALUES (2047351466927181826, 1, '个人信息', 'UPLOAD_AVATAR', 1, 'system', '/api/v1/auth/avatar/upload', 'POST', 'AuthController.uploadAvatar', '[]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":\"iam%2Favatar%2F2026_04_24%2Fa45221b5e95b449596cc3f0def0eb55e.jpg\",\"traceId\":\"trace-1776961602321\",\"timestamp\":\"1776961602321\"}', 0, NULL, 534, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-24 00:26:42');
INSERT INTO `sys_oper_log` VALUES (2047713942252363777, 1, '租户管理', 'CREATE', 1, 'system', '/api/v1/tenants', 'POST', 'TenantController.create', '[{\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"***\",\"expireTime\":\"2026-08-28T00:00:00\",\"domain\":\"\",\"welcomeText\":\"\"}]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"id\":\"2047713939437985793\",\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"13358586565\",\"expireTime\":\"2026-08-28T00:00:00\",\"status\":0,\"domain\":\"\",\"welcomeText\":\"\",\"createdBy\":null,\"createdAt\":\"2026-04-25T00:27:02\",\"updatedBy\":null,\"updatedAt\":\"2026-04-25T00:27:02\",\"deleted\":0},\"traceId\":\"trace-1777048022906\",\"timestamp\":\"1777048022906\"}', 0, NULL, 438, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-25 00:27:03');
INSERT INTO `sys_oper_log` VALUES (2047713981737541634, 1, '租户管理', 'UPDATE', 1, 'system', '/api/v1/tenants/20001', 'PUT', 'TenantController.update', '[\"20001\",{\"tenantName\":\"测试公司A\",\"logoUrl\":\"\",\"shortCode\":\"CSGSA\",\"contactName\":\"李四\",\"phone\":\"***\",\"expireTime\":\"2099-12-31T00:00:00\",\"domain\":\"\",\"welcomeText\":\"\"}]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"id\":\"20001\",\"tenantName\":\"测试公司A\",\"logoUrl\":null,\"shortCode\":\"CSGSA\",\"contactName\":\"李四\",\"phone\":\"13800000002\",\"expireTime\":\"2099-12-31T00:00:00\",\"status\":0,\"domain\":null,\"welcomeText\":null,\"createdBy\":null,\"createdAt\":\"2026-04-17T01:57:30\",\"updatedBy\":null,\"updatedAt\":\"2026-04-17T01:57:30\",\"deleted\":0},\"traceId\":\"trace-1777048032588\",\"timestamp\":\"1777048032588\"}', 0, NULL, 21, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-25 00:27:12');
INSERT INTO `sys_oper_log` VALUES (2047714722174803969, 1, '租户管理', 'UPDATE', 1, 'system', '/api/v1/tenants/2047713939437985793', 'PUT', 'TenantController.update', '[\"2047713939437985793\",{\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"***\",\"expireTime\":\"2026-08-28T00:00:00\",\"domain\":\"哦\",\"welcomeText\":\"\"}]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"id\":\"2047713939437985793\",\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"13358586565\",\"expireTime\":\"2026-08-28T00:00:00\",\"status\":0,\"domain\":\"哦\",\"welcomeText\":\"\",\"createdBy\":null,\"createdAt\":\"2026-04-25T00:27:02\",\"updatedBy\":null,\"updatedAt\":\"2026-04-25T00:27:02\",\"deleted\":0},\"traceId\":\"trace-1777048209119\",\"timestamp\":\"1777048209119\"}', 0, NULL, 439, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-25 00:30:09');
INSERT INTO `sys_oper_log` VALUES (2047714907986681857, 1, '租户管理', 'UPDATE', 1, 'system', '/api/v1/tenants/2047713939437985793', 'PUT', 'TenantController.update', '[\"2047713939437985793\",{\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"***\",\"expireTime\":\"2026-08-28T00:00:00\",\"domain\":\"哦\",\"welcomeText\":\"\"}]', NULL, 1, '域名格式不正确，示例：company.example.com', 15, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-25 00:30:53');
INSERT INTO `sys_oper_log` VALUES (2047715726672879618, 1, '租户管理', 'UPDATE', 1, 'system', '/api/v1/tenants/2047713939437985793', 'PUT', 'TenantController.update', '[\"2047713939437985793\",{\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"***\",\"expireTime\":\"2026-08-28T00:00:00\",\"domain\":\"\",\"welcomeText\":\"\"}]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"id\":\"2047713939437985793\",\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"13358586565\",\"expireTime\":\"2026-08-28T00:00:00\",\"status\":0,\"domain\":\"哦\",\"welcomeText\":\"\",\"createdBy\":null,\"createdAt\":\"2026-04-25T00:27:02\",\"updatedBy\":null,\"updatedAt\":\"2026-04-25T00:27:02\",\"deleted\":0},\"traceId\":\"trace-1777048448609\",\"timestamp\":\"1777048448611\"}', 0, NULL, 46, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-25 00:34:08');
INSERT INTO `sys_oper_log` VALUES (2047716386868908034, 1, '租户管理', 'UPDATE', 1, 'system', '/api/v1/tenants/2047713939437985793', 'PUT', 'TenantController.update', '[\"2047713939437985793\",{\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"***\",\"expireTime\":\"2026-08-28T00:00:00\",\"domain\":\"\",\"welcomeText\":\"\"}]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"id\":\"2047713939437985793\",\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"13358586565\",\"expireTime\":\"2026-08-28T00:00:00\",\"status\":0,\"domain\":\"哦\",\"welcomeText\":\"\",\"createdBy\":null,\"createdAt\":\"2026-04-25T00:27:02\",\"updatedBy\":null,\"updatedAt\":\"2026-04-25T00:27:02\",\"deleted\":0},\"traceId\":\"trace-1777048606003\",\"timestamp\":\"1777048606003\"}', 0, NULL, 66, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-25 00:36:46');
INSERT INTO `sys_oper_log` VALUES (2047716717371592705, 1, '租户管理', 'UPDATE', 1, 'system', '/api/v1/tenants/2047713939437985793', 'PUT', 'TenantController.update', '[\"2047713939437985793\",{\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"***\",\"expireTime\":\"2026-08-28T00:00:00\",\"domain\":\"\",\"welcomeText\":\"\"}]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"id\":\"2047713939437985793\",\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"13358586565\",\"expireTime\":\"2026-08-28T00:00:00\",\"status\":0,\"domain\":\"哦\",\"welcomeText\":\"\",\"createdBy\":null,\"createdAt\":\"2026-04-25T00:27:02\",\"updatedBy\":null,\"updatedAt\":\"2026-04-25T00:27:02\",\"deleted\":0},\"traceId\":\"trace-1777048684801\",\"timestamp\":\"1777048684801\"}', 0, NULL, 65, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-25 00:38:04');
INSERT INTO `sys_oper_log` VALUES (2047719001392521217, 1, '租户管理', 'UPDATE', 1, 'system', '/api/v1/tenants/2047713939437985793', 'PUT', 'TenantController.update', '[\"2047713939437985793\",{\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"***\",\"expireTime\":\"2026-08-28T00:00:00\",\"domain\":\"\",\"welcomeText\":\"\"}]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"id\":\"2047713939437985793\",\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"13358586565\",\"expireTime\":\"2026-08-28T00:00:00\",\"status\":0,\"domain\":\"哦\",\"welcomeText\":\"\",\"createdBy\":null,\"createdAt\":\"2026-04-25T00:27:02\",\"updatedBy\":null,\"updatedAt\":\"2026-04-25T00:27:02\",\"deleted\":0},\"traceId\":\"trace-1777049229355\",\"timestamp\":\"1777049229355\"}', 0, NULL, 68, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-25 00:47:09');
INSERT INTO `sys_oper_log` VALUES (2047719514431369218, 1, '租户管理', 'UPDATE', 1, 'system', '/api/v1/tenants/2047713939437985793', 'PUT', 'TenantController.update', '[\"2047713939437985793\",{\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"***\",\"expireTime\":\"2026-08-28T00:00:00\",\"domain\":\"\",\"welcomeText\":\"\"}]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"id\":\"2047713939437985793\",\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"13358586565\",\"expireTime\":\"2026-08-28T00:00:00\",\"status\":0,\"domain\":\"哦\",\"welcomeText\":\"\",\"createdBy\":null,\"createdAt\":\"2026-04-25T00:27:02\",\"updatedBy\":null,\"updatedAt\":\"2026-04-25T00:27:02\",\"deleted\":0},\"traceId\":\"trace-1777049351674\",\"timestamp\":\"1777049351674\"}', 0, NULL, 180, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-25 00:49:11');
INSERT INTO `sys_oper_log` VALUES (2047719789183496193, 1, '租户管理', 'UPDATE', 1, 'system', '/api/v1/tenants/2047713939437985793', 'PUT', 'TenantController.update', '[\"2047713939437985793\",{\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"***\",\"expireTime\":\"2026-08-28T00:00:00\",\"domain\":\"\",\"welcomeText\":\"\"}]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"id\":\"2047713939437985793\",\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"13358586565\",\"expireTime\":\"2026-08-28T00:00:00\",\"status\":0,\"domain\":\"\",\"welcomeText\":\"\",\"createdBy\":null,\"createdAt\":\"2026-04-25T00:27:02\",\"updatedBy\":null,\"updatedAt\":\"2026-04-25T00:27:02\",\"deleted\":0},\"traceId\":\"trace-1777049417175\",\"timestamp\":\"1777049417175\"}', 0, NULL, 180, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-25 00:50:17');
INSERT INTO `sys_oper_log` VALUES (2047730439804809218, 1, '租户管理', 'UPDATE', 1, 'system', '/api/v1/tenants/2047713939437985793', 'PUT', 'TenantController.update', '[\"2047713939437985793\",{\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"***\",\"expireTime\":\"2026-08-28T00:00:00\",\"domain\":\"2\",\"welcomeText\":\"\"}]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"id\":\"2047713939437985793\",\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"13358586565\",\"expireTime\":\"2026-08-28T00:00:00\",\"status\":0,\"domain\":\"2\",\"welcomeText\":\"\",\"createdBy\":null,\"createdAt\":\"2026-04-25T00:27:02\",\"updatedBy\":null,\"updatedAt\":\"2026-04-25T00:27:02\",\"deleted\":0},\"traceId\":\"trace-1777051956492\",\"timestamp\":\"1777051956492\"}', 0, NULL, 417, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-25 01:32:36');
INSERT INTO `sys_oper_log` VALUES (2047730625402761218, 1, '租户管理', 'UPDATE', 1, 'system', '/api/v1/tenants/2047713939437985793', 'PUT', 'TenantController.update', '[\"2047713939437985793\",{\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"***\",\"expireTime\":\"2026-08-28T00:00:00\",\"domain\":\"\",\"welcomeText\":\"\"}]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"id\":\"2047713939437985793\",\"tenantName\":\"软科政企\",\"logoUrl\":\"\",\"shortCode\":\"RKZQ\",\"contactName\":\"李武\",\"phone\":\"13358586565\",\"expireTime\":\"2026-08-28T00:00:00\",\"status\":0,\"domain\":\"\",\"welcomeText\":\"\",\"createdBy\":null,\"createdAt\":\"2026-04-25T00:27:02\",\"updatedBy\":null,\"updatedAt\":\"2026-04-25T00:27:02\",\"deleted\":0},\"traceId\":\"trace-1777052000744\",\"timestamp\":\"1777052000744\"}', 0, NULL, 415, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-25 01:33:20');
INSERT INTO `sys_oper_log` VALUES (2048771281378086913, 1, '用户管理', 'RESET_PASSWORD', 1, 'system', '/api/v1/users/10001/reset-password', 'PATCH', 'UserController.resetPassword', '[\"10001\"]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"userId\":\"10001\",\"newPassword\":\"Init@ZHGLY952\",\"needChange\":true,\"sessionsInvalidated\":true},\"traceId\":\"trace-1777300112409\",\"timestamp\":\"1777300112409\"}', 0, NULL, 452, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-27 22:28:32');
INSERT INTO `sys_oper_log` VALUES (2048771353734025218, 1, '用户管理', 'RESET_PASSWORD', 1, 'system', '/api/v1/users/10002/reset-password', 'PATCH', 'UserController.resetPassword', '[\"10002\"]', '{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"userId\":\"10002\",\"newPassword\":\"Init@ZS628\",\"needChange\":true,\"sessionsInvalidated\":true},\"traceId\":\"trace-1777300129717\",\"timestamp\":\"1777300129717\"}', 0, NULL, 111, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0', '2026-04-27 22:28:49');

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, 1, '系统管理员', 'PLATFORM_ADMIN', 1, 0, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_role` VALUES (2, 0, '租户管理员', 'TENANT_ADMIN', 2, 0, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `sys_role` VALUES (3, 0, '普通用户', 'USER', 4, 0, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色菜单关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (1, 0, 1, 1, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (2, 0, 1, 100, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (10, 0, 1, 10, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (11, 0, 1, 11, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (12, 0, 1, 110, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (13, 0, 1, 111, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (14, 0, 1, 112, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (15, 0, 1, 113, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (20, 0, 1, 12, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (21, 0, 1, 120, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (22, 0, 1, 121, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (23, 0, 1, 122, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (24, 0, 1, 123, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (25, 0, 1, 124, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (26, 0, 1, 125, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (27, 0, 1, 126, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (28, 0, 1, 127, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (30, 0, 1, 13, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (31, 0, 1, 130, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (32, 0, 1, 131, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (33, 0, 1, 132, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (34, 0, 1, 133, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (35, 0, 1, 134, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (40, 0, 1, 14, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (41, 0, 1, 140, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (42, 0, 1, 141, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (43, 0, 1, 142, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (44, 0, 1, 143, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (50, 0, 1, 15, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (51, 0, 1, 150, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (52, 0, 1, 151, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (53, 0, 1, 152, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (54, 0, 1, 153, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (60, 0, 1, 20, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (61, 0, 1, 21, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (62, 0, 1, 210, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (63, 0, 1, 211, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (70, 0, 1, 22, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (71, 0, 1, 220, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (72, 0, 1, 221, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (73, 0, 1, 222, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (74, 0, 1, 223, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (75, 0, 1, 224, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (76, 0, 1, 225, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (80, 0, 1, 30, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (81, 0, 1, 300, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (82, 0, 1, 301, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (83, 0, 1, 302, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (84, 0, 1, 303, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (200, 0, 2, 1, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (201, 0, 2, 100, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (210, 0, 2, 12, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (211, 0, 2, 120, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (212, 0, 2, 121, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (213, 0, 2, 122, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (214, 0, 2, 123, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (215, 0, 2, 124, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (216, 0, 2, 125, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (220, 0, 2, 13, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (221, 0, 2, 130, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (222, 0, 2, 131, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (223, 0, 2, 132, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (224, 0, 2, 133, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (230, 0, 2, 20, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (231, 0, 2, 21, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (232, 0, 2, 210, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (233, 0, 2, 22, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (234, 0, 2, 220, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (240, 0, 2, 30, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (241, 0, 2, 300, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (242, 0, 2, 301, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (243, 0, 2, 302, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (244, 0, 2, 303, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (300, 0, 3, 1, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (301, 0, 3, 100, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (310, 0, 3, 12, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (311, 0, 3, 120, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (320, 0, 3, 22, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (321, 0, 3, 220, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (330, 0, 3, 30, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (331, 0, 3, 300, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (332, 0, 3, 301, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (333, 0, 3, 302, '2026-04-19 00:00:00');

-- ----------------------------
-- Table structure for sys_tenant
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户名称',
  `logo_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '企业Logo URL',
  `short_code` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户简称',
  `contact_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联系人',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联系电话',
  `expire_time` datetime NOT NULL COMMENT '到期时间',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态:0正常 1冻结',
  `domain` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '自定义域名（如 company.example.com）',
  `welcome_text` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '租户自定义配置（JSON格式：欢迎语、主题色、功能开关等）',
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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户信息表（SaaS多租户）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_tenant
-- ----------------------------
INSERT INTO `sys_tenant` VALUES (1, '智创科技', 'common%2F2026_04_24%2F7bc6100a6c5347eca5fa27a784645464.webp', 'ZC', '张坤', '18375219477', '2099-12-31 00:00:00', 0, NULL, '{\n  \"welcome\": \"欢迎{username}使用XX电镀ERP系统\",\n  \"features\": {\n    \"enableMobile\": true,\n    \"enableSSO\": false\n  },\n  \"footerText\": \"© 2024 XX电镀科技有限公司\"\n}', NULL, '2026-04-17 01:57:29', NULL, '2026-04-17 01:57:29', 0);
INSERT INTO `sys_tenant` VALUES (20001, '测试公司A', NULL, 'CSGSA', '李四', '13800000002', '2099-12-31 00:00:00', 0, NULL, NULL, NULL, '2026-04-17 01:57:30', NULL, '2026-04-17 01:57:30', 0);
INSERT INTO `sys_tenant` VALUES (2047713939437985793, '软科政企', '', 'RKZQ', '李武', '13358586565', '2026-08-28 00:00:00', 0, '', '', NULL, '2026-04-25 00:27:02', NULL, '2026-04-25 00:27:02', 0);

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
  `position` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '岗位/职位',
  `leader_user_id` bigint NULL DEFAULT NULL COMMENT '直属领导用户ID',
  `office_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '办公电话',
  `join_date` date NULL DEFAULT NULL COMMENT '入职日期',
  `employee_status` tinyint NOT NULL DEFAULT 0 COMMENT '员工状态:0在职 1离职 2试用期',
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
  INDEX `idx_tenant_dept`(`tenant_id`, `dept_id`) USING BTREE,
  INDEX `idx_leader_user`(`leader_user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表（包含员工信息）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 1, 'system', '$2a$10$7cR05yaoxCH8PeyGGEA/C.UTyfsSzsa7Fuy9jdbbVZ/MgVyoOqwq2', '张坤', 'iam%2Favatar%2F2026_04_24%2Fa45221b5e95b449596cc3f0def0eb55e.jpg', NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, 0, 0, '2026-04-27 23:38:52', '127.0.0.1', 23, NULL, NULL, NULL, '2026-04-17 01:57:30', NULL, '2026-04-27 23:38:52', 0);
INSERT INTO `sys_user` VALUES (10001, 20001, 'a-admin', '$2a$10$vh7HJgPPXtOy5P54CWzcy.dxK.xx/KeH4qosHhumu5bzge2/dmH9i', '租户管理员', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, 1, 0, '2026-04-20 23:56:36', '127.0.0.1', 3, NULL, NULL, NULL, '2026-04-17 01:57:30', 1, '2026-04-20 23:56:35', 0);
INSERT INTO `sys_user` VALUES (10002, 20001, 'a-user', '$2a$10$dgiFuTvJHl3T0fzOBVA8qObGpiEtirRZxUcCPgL8hrn6RyODDesZG', '张三', NULL, NULL, NULL, NULL, NULL, NULL, 0, '13800000003', 'zhangsan@demo.com', 1, 0, NULL, NULL, 0, NULL, NULL, NULL, '2026-04-17 01:57:35', 1, '2026-04-17 01:57:35', 0);

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户最近登录租户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_recent_tenant
-- ----------------------------
INSERT INTO `sys_user_recent_tenant` VALUES (2046247815215153154, 10001, 20001, '2026-04-20 23:56:36', 3, '2026-04-20 23:21:11', '2026-04-20 23:56:36');

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1, 1, 1, '2026-04-20 23:56:19');
INSERT INTO `sys_user_role` VALUES (50001, 20001, 10001, 2, '2026-04-17 01:57:33');
INSERT INTO `sys_user_role` VALUES (50002, 20001, 10002, 3, '2026-04-17 01:57:35');

SET FOREIGN_KEY_CHECKS = 1;
