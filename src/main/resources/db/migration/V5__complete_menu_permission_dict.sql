
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- V5: 补全菜单体系、权限管理、字典管理
-- =====================================================

-- ----------------------------
-- 1. 更新 sys_menu 表结构（补充缺失字段）
-- ----------------------------
ALTER TABLE `sys_menu` 
  ADD COLUMN `icon` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '菜单图标' AFTER `path`,
  ADD COLUMN `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序' AFTER `status`,
  ADD COLUMN `visible` tinyint NOT NULL DEFAULT 1 COMMENT '是否可见:0隐藏 1显示' AFTER `sort_no`,
  ADD COLUMN `component` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '组件路径' AFTER `visible`,
  ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID(0=平台级)' AFTER `parent_id`,
  ADD COLUMN `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志:0正常 1删除' AFTER `updated_at`;

-- 更新索引
ALTER TABLE `sys_menu` 
  ADD INDEX `idx_tenant_parent`(`tenant_id`, `parent_id`) USING BTREE,
  ADD INDEX `idx_tenant_status`(`tenant_id`, `status`) USING BTREE;

-- ----------------------------
-- 2. 补全菜单数据（按照现有功能补全）
-- ----------------------------

-- 先清空旧数据（父ID都需要更新）
DELETE FROM `sys_menu`;

-- 平台级公共菜单（tenant_id=0，所有租户共享）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tenant_id`, `menu_name`, `menu_type`, `path`, `icon`, `perms`, `sort_no`, `status`, `visible`, `component`, `created_at`, `updated_at`) VALUES
(1, 0, 0, '系统管理', 'M', '/', 'OfficeBuilding', NULL, 0, 0, 1, 'DashboardView', '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(2, 1, 0, '租户管理', 'C', 'tenants', 'OfficeBuilding', NULL, 0, 0, 1, 'TenantView', '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(3, 1, 0, '用户管理', 'C', 'users', 'User', NULL, 0, 0, 1, 'UserView', '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(4, 1, 0, '角色管理', 'C', 'roles', 'Key', NULL, 0, 0, 1, 'RoleView', '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(5, 1, 0, '菜单管理', 'C', 'menus', 'Menu', NULL, 0, 0, 1, 'MenuView', '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(6, 1, 0, '日志中心', 'C', 'logs', 'Document', NULL, 0, 0, 1, 'LogView', '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(7, 1, 0, '消息管理', 'C', 'messages', 'Bell', NULL, 0, 0, 1, 'MessageView', '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(8, 1, 0, '字典管理', 'C', 'dicts', 'Notebook', NULL, 0, 0, 1, 'DictView', '2026-04-19 00:00:00', '2026-04-19 00:00:00');

-- 租户管理按钮权限
INSERT INTO `sys_menu` (`id`, `parent_id`, `tenant_id`, `menu_name`, `menu_type`, `path`, `icon`, `perms`, `sort_no`, `status`, `visible`, `component`, `created_at`, `updated_at`) VALUES
(100, 2, 0, '租户查看', 'F', '', '', 'tenant:view', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(101, 2, 0, '租户新增', 'F', '', '', 'tenant:add', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(102, 2, 0, '租户编辑', 'F', '', '', 'tenant:edit', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(103, 2, 0, '租户状态', 'F', '', '', 'tenant:status', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00');

-- 用户管理按钮权限
INSERT INTO `sys_menu` (`id`, `parent_id`, `tenant_id`, `menu_name`, `menu_type`, `path`, `icon`, `perms`, `sort_no`, `status`, `visible`, `component`, `created_at`, `updated_at`) VALUES
(200, 3, 0, '用户查看', 'F', '', '', 'user:view', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(201, 3, 0, '用户新增', 'F', '', '', 'user:add', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(202, 3, 0, '用户编辑', 'F', '', '', 'user:edit', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(203, 3, 0, '用户状态', 'F', '', '', 'user:status', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(204, 3, 0, '用户重置', 'F', '', '', 'user:reset', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00');

-- 角色管理按钮权限
INSERT INTO `sys_menu` (`id`, `parent_id`, `tenant_id`, `menu_name`, `menu_type`, `path`, `icon`, `perms`, `sort_no`, `status`, `visible`, `component`, `created_at`, `updated_at`) VALUES
(300, 4, 0, '角色查看', 'F', '', '', 'role:view', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(301, 4, 0, '角色新增', 'F', '', '', 'role:add', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(302, 4, 0, '角色编辑', 'F', '', '', 'role:edit', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(303, 4, 0, '角色授权', 'F', '', '', 'role:grant', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(304, 4, 0, '角色删除', 'F', '', '', 'role:delete', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00');

-- 菜单管理按钮权限
INSERT INTO `sys_menu` (`id`, `parent_id`, `tenant_id`, `menu_name`, `menu_type`, `path`, `icon`, `perms`, `sort_no`, `status`, `visible`, `component`, `created_at`, `updated_at`) VALUES
(400, 5, 0, '菜单查看', 'F', '', '', 'menu:view', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(401, 5, 0, '菜单新增', 'F', '', '', 'menu:add', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(402, 5, 0, '菜单编辑', 'F', '', '', 'menu:edit', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(403, 5, 0, '菜单删除', 'F', '', '', 'menu:delete', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00');

-- 日志中心按钮权限
INSERT INTO `sys_menu` (`id`, `parent_id`, `tenant_id`, `menu_name`, `menu_type`, `path`, `icon`, `perms`, `sort_no`, `status`, `visible`, `component`, `created_at`, `updated_at`) VALUES
(500, 6, 0, '日志查看', 'F', '', '', 'log:view', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(501, 6, 0, '日志导出', 'F', '', '', 'log:export', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00');

-- 消息管理按钮权限
INSERT INTO `sys_menu` (`id`, `parent_id`, `tenant_id`, `menu_name`, `menu_type`, `path`, `icon`, `perms`, `sort_no`, `status`, `visible`, `component`, `created_at`, `updated_at`) VALUES
(600, 7, 0, '消息查看', 'F', '', '', 'message:view', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(601, 7, 0, '消息新增', 'F', '', '', 'message:add', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(602, 7, 0, '消息发布', 'F', '', '', 'message:publish', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(603, 7, 0, '消息撤回', 'F', '', '', 'message:revoke', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(604, 7, 0, '邮件记录查看', 'F', '', '', 'message:email:view', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00');

-- 字典管理按钮权限
INSERT INTO `sys_menu` (`id`, `parent_id`, `tenant_id`, `menu_name`, `menu_type`, `path`, `icon`, `perms`, `sort_no`, `status`, `visible`, `component`, `created_at`, `updated_at`) VALUES
(700, 8, 0, '字典查看', 'F', '', '', 'dict:view', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(701, 8, 0, '字典新增', 'F', '', '', 'dict:add', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(702, 8, 0, '字典编辑', 'F', '', '', 'dict:edit', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(703, 8, 0, '字典删除', 'F', '', '', 'dict:delete', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00');

-- 个人信息菜单（所有用户可见，tenant_id=0）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tenant_id`, `menu_name`, `menu_type`, `path`, `icon`, `perms`, `sort_no`, `status`, `visible`, `component`, `created_at`, `updated_at`) VALUES
(900, 0, 0, '个人中心', 'C', 'profile', 'Avatar', NULL, 0, 0, 1, 'ProfileView', '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(901, 900, 0, '查看个人信息', 'F', '', '', 'profile:view', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(902, 900, 0, '编辑个人信息', 'F', '', '', 'profile:edit', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(903, 900, 0, '修改密码', 'F', '', '', 'profile:password', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00'),
(904, 900, 0, '编辑公司信息', 'F', '', '', 'company:edit', 0, 0, 1, NULL, '2026-04-19 00:00:00', '2026-04-19 00:00:00');

-- ----------------------------
-- 3. 更新 sys_role_menu 关联关系（使用新菜单ID）
-- ----------------------------

-- 清空旧数据
DELETE FROM `sys_role_menu`;

-- 系统管理员角色（role_id=1，tenant_id=1）拥有所有平台级权限
INSERT INTO `sys_role` VALUES (1, 1, '系统管理员', 'PLATFORM_ADMIN', 1, 0, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);

-- 系统管理员关联所有平台级菜单（ID 1-8为目录和菜单）
INSERT INTO `sys_role_menu` VALUES (1, 1, 1, 1, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (2, 1, 1, 2, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (3, 1, 1, 100, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (4, 1, 1, 101, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (5, 1, 1, 102, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (6, 1, 1, 103, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (7, 1, 1, 3, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (8, 1, 1, 200, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (9, 1, 1, 201, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (10, 1, 1, 202, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (11, 1, 1, 203, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (12, 1, 1, 204, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (13, 1, 1, 4, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (14, 1, 1, 300, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (15, 1, 1, 301, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (16, 1, 1, 302, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (17, 1, 1, 303, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (18, 1, 1, 304, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (19, 1, 1, 5, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (20, 1, 1, 400, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (21, 1, 1, 401, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (22, 1, 1, 402, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (23, 1, 1, 403, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (24, 1, 1, 6, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (25, 1, 1, 500, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (26, 1, 1, 501, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (27, 1, 1, 7, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (28, 1, 1, 600, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (29, 1, 1, 601, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (30, 1, 1, 602, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (31, 1, 1, 603, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (32, 1, 1, 604, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (33, 1, 1, 8, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (34, 1, 1, 700, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (35, 1, 1, 701, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (36, 1, 1, 702, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (37, 1, 1, 703, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (38, 1, 1, 900, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (39, 1, 1, 901, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (40, 1, 1, 902, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (41, 1, 1, 903, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (42, 1, 1, 904, '2026-04-19 00:00:00');

-- 租户管理员角色（role_id=40001）拥有所有权限
INSERT INTO `sys_role_menu` VALUES (43, 20001, 40001, 1, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (44, 20001, 40001, 2, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (45, 20001, 40001, 100, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (46, 20001, 40001, 101, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (47, 20001, 40001, 102, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (48, 20001, 40001, 103, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (49, 20001, 40001, 3, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (50, 20001, 40001, 200, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (51, 20001, 40001, 201, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (52, 20001, 40001, 202, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (53, 20001, 40001, 203, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (54, 20001, 40001, 204, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (55, 20001, 40001, 4, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (56, 20001, 40001, 300, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (57, 20001, 40001, 301, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (58, 20001, 40001, 302, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (59, 20001, 40001, 303, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (60, 20001, 40001, 304, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (61, 20001, 40001, 5, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (62, 20001, 40001, 400, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (63, 20001, 40001, 401, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (64, 20001, 40001, 402, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (65, 20001, 40001, 403, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (66, 20001, 40001, 6, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (67, 20001, 40001, 500, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (68, 20001, 40001, 501, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (69, 20001, 40001, 7, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (70, 20001, 40001, 600, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (71, 20001, 40001, 601, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (72, 20001, 40001, 602, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (73, 20001, 40001, 603, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (74, 20001, 40001, 604, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (75, 20001, 40001, 8, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (76, 20001, 40001, 700, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (77, 20001, 40001, 701, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (78, 20001, 40001, 702, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (79, 20001, 40001, 703, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (80, 20001, 40001, 900, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (81, 20001, 40001, 901, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (82, 20001, 40001, 902, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (83, 20001, 40001, 903, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (84, 20001, 40001, 904, '2026-04-19 00:00:00');

-- 普通员工角色（role_id=40002）只有查看权限
INSERT INTO `sys_role_menu` VALUES (85, 20001, 40002, 200, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (86, 20001, 40002, 300, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (87, 20001, 40002, 400, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (88, 20001, 40002, 500, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (89, 20001, 40002, 600, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (90, 20001, 40002, 604, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (91, 20001, 40002, 700, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (92, 20001, 40002, 900, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (93, 20001, 40002, 901, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (94, 20001, 40002, 902, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (95, 20001, 40002, 903, '2026-04-19 00:00:00');
INSERT INTO `sys_role_menu` VALUES (96, 20001, 40002, 904, '2026-04-19 00:00:00');

-- ----------------------------
-- 4. 更新 base_dict_type 表结构（补充字段）
-- ----------------------------
ALTER TABLE `base_dict_type` 
  ADD COLUMN `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序' AFTER `status`;

-- 补充字典类型数据
INSERT INTO `base_dict_type` (`id`, `tenant_id`, `dict_type`, `dict_name`, `status`, `sort_no`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted`) VALUES (6, 0, 'sys_menu_type', '菜单类型', 0, 0, 'M=目录 C=菜单 F=按钮', NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `base_dict_type` (`id`, `tenant_id`, `dict_type`, `dict_name`, `status`, `sort_no`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted`) VALUES (7, 0, 'sys_status', '通用状态', 0, 0, '0=正常 1=停用', NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `base_dict_type` (`id`, `tenant_id`, `dict_type`, `dict_name`, `status`, `sort_no`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted`) VALUES (8, 0, 'sys_yes_no', '是否', 0, 0, '0=否 1=是', NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);

-- 补充字典项数据
INSERT INTO `base_dict_item` VALUES (24, 0, 'sys_menu_type', '目录', 'M', 1, 0, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (25, 0, 'sys_menu_type', '菜单', 'C', 2, 0, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (26, 0, 'sys_menu_type', '按钮', 'F', 3, 0, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (27, 0, 'sys_status', '正常', '0', 1, 0, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (28, 0, 'sys_status', '停用', '1', 2, 0, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (29, 0, 'sys_yes_no', '否', '0', 1, 0, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (30, 0, 'sys_yes_no', '是', '1', 2, 0, NULL, '2026-04-19 00:00:00', NULL, '2026-04-19 00:00:00', 0);

SET FOREIGN_KEY_CHECKS = 1;
