-- 为sys_user表添加扩展字段（岗位、直属领导等）
-- 注意：username即为工号，不需要额外添加employee_no字段
ALTER TABLE `sys_user`
ADD COLUMN `position` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '岗位/职位' AFTER `dept_id`,
ADD COLUMN `leader_user_id` bigint NULL DEFAULT NULL COMMENT '直属领导用户ID' AFTER `position`,
ADD COLUMN `office_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '办公电话' AFTER `leader_user_id`,
ADD COLUMN `join_date` date NULL DEFAULT NULL COMMENT '入职日期' AFTER `office_phone`,
ADD COLUMN `employee_status` tinyint NOT NULL DEFAULT 0 COMMENT '员工状态:0在职 1离职 2试用期' AFTER `join_date`,
ADD INDEX `idx_leader_user`(`leader_user_id`) USING BTREE;

-- 添加注释说明
ALTER TABLE `sys_user` COMMENT = '用户表（包含员工信息）';
