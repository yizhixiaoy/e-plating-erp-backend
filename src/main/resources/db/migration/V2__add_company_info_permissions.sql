
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 添加公司信息查看和编辑权限
-- ----------------------------

-- 添加公司信息相关菜单权限
INSERT INTO `sys_menu` VALUES (60026, 0, '公司信息查看', 'F', '/profile', 'company:view', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');
INSERT INTO `sys_menu` VALUES (60027, 0, '公司信息编辑', 'F', '/profile', 'company:edit', 0, NULL, '2026-04-17 01:57:33', NULL, '2026-04-17 01:57:33');

-- ----------------------------
-- 为租户管理员角色分配公司信息权限
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (130026, 20001, 40001, 60026, '2026-04-17 01:57:33');
INSERT INTO `sys_role_menu` VALUES (130027, 20001, 40001, 60027, '2026-04-17 01:57:33');

SET FOREIGN_KEY_CHECKS = 1;
