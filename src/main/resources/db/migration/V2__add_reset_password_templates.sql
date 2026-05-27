-- 重置密码邮件模板（区别于新用户注册模板）
INSERT INTO `base_dict_item` VALUES (361, 0, 'email_template', '重置密码邮件主题', '密码重置通知 - {tenantName}', 7, 0, NULL, '2026-05-27 00:00:00', NULL, '2026-05-27 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (362, 0, 'email_template', '重置密码邮件内容', '尊敬的{realName}:\n\n您的账号密码已被管理员重置。\n\n账号信息如下:\n用户名: {username}\n新密码: {password}\n登录地址: {loginUrl}\n\n请使用新密码登录后，尽快修改为您的个人密码。', 8, 0, NULL, '2026-05-27 00:00:00', NULL, '2026-05-27 00:00:00', 0);

-- 邮件推送配置增加重置密码通知
INSERT INTO `base_dict_item` VALUES (363, 0, 'email_push_config', '重置密码邮件通知', 'reset_password', 4, 0, NULL, '2026-05-27 00:00:00', NULL, '2026-05-27 00:00:00', 0);
