-- ============================================
-- V6: sys_role 增加 dept_id 字段
-- 需求：角色绑定到公司和部门，实现公司+部门级别的角色隔离
-- ============================================

ALTER TABLE `sys_role`
    ADD COLUMN `dept_id` bigint NULL COMMENT '部门ID' AFTER `status`;

-- 回滚脚本（如需）
-- ALTER TABLE `sys_role` DROP COLUMN `dept_id`;
