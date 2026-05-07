-- ============================================
-- V4: 放宽用户手机号唯一约束
-- 允许同公司内不同用户共用手机号（如：张三-183...、张三2-183...）
-- 仅当 同名+同手机号+同公司 时才视为重复人员
-- ============================================

-- 删除旧的 租户+手机号 唯一约束（过于严格）
ALTER TABLE `sys_user`
  DROP INDEX `uk_tenant_phone`;

-- 新增 租户+姓名+手机号 联合唯一约束
-- MySQL 对 NULL 值不判定重复，空手机号的不同用户不会被误拦
ALTER TABLE `sys_user`
  ADD UNIQUE INDEX `uk_tenant_realname_phone` (`tenant_id`, `real_name`, `phone`) USING BTREE;
