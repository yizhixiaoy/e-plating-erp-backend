-- ============================================
-- V2: 部门与岗位管理
-- ============================================

-- ----------------------------
-- Table: sys_position（岗位表）
-- ----------------------------
DROP TABLE IF EXISTS `sys_position`;
CREATE TABLE `sys_position` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `position_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位名称',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态:0正常 1停用',
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_tenant_position` (`tenant_id`, `position_name`) USING BTREE,
  INDEX `idx_tenant_status` (`tenant_id`, `status`) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位表';

-- ----------------------------
-- 菜单：部门管理
-- ----------------------------
INSERT INTO `sys_menu` VALUES (16, 10, 0, '部门管理', 'C', 'depts', 'Office', 'dept:view', 0, 5, 1, 'DeptView', NULL, NOW(), NULL, NOW(), 0);
INSERT INTO `sys_menu` VALUES (160, 16, 0, '部门查看', 'F', '', '', 'dept:view', 0, 0, 1, NULL, NULL, NOW(), NULL, NOW(), 0);
INSERT INTO `sys_menu` VALUES (161, 16, 0, '部门新增', 'F', '', '', 'dept:add', 0, 1, 1, NULL, NULL, NOW(), NULL, NOW(), 0);
INSERT INTO `sys_menu` VALUES (162, 16, 0, '部门编辑', 'F', '', '', 'dept:edit', 0, 2, 1, NULL, NULL, NOW(), NULL, NOW(), 0);
INSERT INTO `sys_menu` VALUES (163, 16, 0, '部门删除', 'F', '', '', 'dept:delete', 0, 3, 1, NULL, NULL, NOW(), NULL, NOW(), 0);

-- ----------------------------
-- 菜单：岗位管理
-- ----------------------------
INSERT INTO `sys_menu` VALUES (17, 10, 0, '岗位管理', 'C', 'positions', 'Briefcase', 'position:view', 0, 6, 1, 'PositionView', NULL, NOW(), NULL, NOW(), 0);
INSERT INTO `sys_menu` VALUES (170, 17, 0, '岗位查看', 'F', '', '', 'position:view', 0, 0, 1, NULL, NULL, NOW(), NULL, NOW(), 0);
INSERT INTO `sys_menu` VALUES (171, 17, 0, '岗位新增', 'F', '', '', 'position:add', 0, 1, 1, NULL, NULL, NOW(), NULL, NOW(), 0);
INSERT INTO `sys_menu` VALUES (172, 17, 0, '岗位编辑', 'F', '', '', 'position:edit', 0, 2, 1, NULL, NULL, NOW(), NULL, NOW(), 0);
INSERT INTO `sys_menu` VALUES (173, 17, 0, '岗位删除', 'F', '', '', 'position:delete', 0, 3, 1, NULL, NULL, NOW(), NULL, NOW(), 0);

-- ----------------------------
-- 角色权限绑定：系统管理员（roleId=1）获得所有新菜单
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES
(600, 0, 1, 16, NOW()),
(601, 0, 1, 160, NOW()),
(602, 0, 1, 161, NOW()),
(603, 0, 1, 162, NOW()),
(604, 0, 1, 163, NOW()),
(610, 0, 1, 17, NOW()),
(611, 0, 1, 170, NOW()),
(612, 0, 1, 171, NOW()),
(613, 0, 1, 172, NOW()),
(614, 0, 1, 173, NOW());

-- ----------------------------
-- 角色权限绑定：租户管理员（roleId=2）获得部门+岗位查看/新增/编辑权限
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES
(620, 0, 2, 16, NOW()),
(621, 0, 2, 160, NOW()),
(622, 0, 2, 161, NOW()),
(623, 0, 2, 162, NOW()),
(624, 0, 2, 163, NOW()),
(630, 0, 2, 17, NOW()),
(631, 0, 2, 170, NOW()),
(632, 0, 2, 171, NOW()),
(633, 0, 2, 172, NOW()),
(634, 0, 2, 173, NOW());
