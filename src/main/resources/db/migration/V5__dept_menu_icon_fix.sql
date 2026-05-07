 -- ============================================
-- V5: 部门管理图标修正 — 避免与租户管理冲突
-- 租户管理(id=11) 已使用 OfficeBuilding 图标
-- 部门管理(id=16) 改用 Connection（组织结构）
-- ============================================

UPDATE `sys_menu` SET `icon` = 'Connection' WHERE `id` = 16 AND `icon` = 'OfficeBuilding';
