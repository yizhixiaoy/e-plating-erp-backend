-- ============================================
-- V3: 岗位-部门关联、租户名唯一约束
-- ============================================

-- ----------------------------
-- sys_position: 添加部门关联字段
-- ----------------------------
-- 1) 添加 dept_id 列
ALTER TABLE `sys_position`
  ADD COLUMN `dept_id` bigint NOT NULL DEFAULT 0 COMMENT '部门ID' AFTER `tenant_id`;

-- 2) 去掉旧的  租户+岗位名  唯一约束，换成  租户+部门+岗位名
ALTER TABLE `sys_position`
  DROP INDEX `uk_tenant_position`,
  ADD UNIQUE INDEX `uk_tenant_dept_position` (`tenant_id`, `dept_id`, `position_name`) USING BTREE;

-- 3) 部门ID索引
ALTER TABLE `sys_position`
  ADD INDEX `idx_dept_id` (`dept_id`) USING BTREE;

-- ----------------------------
-- 菜单图标修正（Office → OfficeBuilding, Briefcase → Stamp）
-- 这两个才是 Element Plus 真实存在的图标
-- ----------------------------
UPDATE `sys_menu` SET `icon` = 'OfficeBuilding' WHERE `id` = 16 AND `icon` = 'Office';
UPDATE `sys_menu` SET `icon` = 'Stamp'         WHERE `id` = 17 AND `icon` = 'Briefcase';

-- ----------------------------
-- 租户名称唯一约束：防止同名公司
-- ----------------------------
ALTER TABLE `sys_tenant`
  ADD UNIQUE INDEX `uk_tenant_name` (`tenant_name`) USING BTREE;
