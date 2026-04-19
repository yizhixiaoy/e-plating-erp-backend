-- 优化sys_tenant表字段
-- 1. 删除avatar_url字段（与logo_url重复）
-- 2. 优化domain字段注释（自定义域名）
-- 3. 优化welcome_text字段为tenant_config（JSON配置）

ALTER TABLE `sys_tenant`
DROP COLUMN `avatar_url`,
MODIFY COLUMN `domain` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '自定义域名（如 company.example.com）',
MODIFY COLUMN `welcome_text` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '租户自定义配置（JSON格式：欢迎语、主题色、功能开关等）';

-- 更新表注释
ALTER TABLE `sys_tenant` COMMENT = '租户信息表（SaaS多租户）';
