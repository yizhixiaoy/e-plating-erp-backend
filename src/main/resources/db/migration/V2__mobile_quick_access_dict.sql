-- ===================== 移动端快捷入口字典 =====================
-- dict_type: mobile_quick_access
-- dict_value 为功能 key（前端路由用），dict_label 为显示名称

INSERT INTO `base_dict_type` VALUES (214, 0, 'mobile_quick_access', '移动端快捷入口', 0, 0, '移动端工作台快捷入口配置', NULL, '2026-06-25 00:00:00', NULL, '2026-06-25 00:00:00', 0);

INSERT INTO `base_dict_item` VALUES (3300, 0, 'mobile_quick_access', '扫一扫',   'scan',     1, 0, NULL, '2026-06-25 00:00:00', NULL, '2026-06-25 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (3301, 0, 'mobile_quick_access', 'AI助手',  'ai',       2, 0, NULL, '2026-06-25 00:00:00', NULL, '2026-06-25 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (3302, 0, 'mobile_quick_access', '聊天',    'chat',     3, 0, NULL, '2026-06-25 00:00:00', NULL, '2026-06-25 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (3303, 0, 'mobile_quick_access', '通讯录',  'contacts', 4, 0, NULL, '2026-06-25 00:00:00', NULL, '2026-06-25 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (3304, 0, 'mobile_quick_access', '消息',    'message',  5, 0, NULL, '2026-06-25 00:00:00', NULL, '2026-06-25 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (3305, 0, 'mobile_quick_access', '待办',    'todo',     6, 0, NULL, '2026-06-25 00:00:00', NULL, '2026-06-25 00:00:00', 0);

-- ===================== 发送状态字典 =====================
-- dict_type: send_status
-- 0=待发送 1=已发送 2=发送失败

INSERT INTO `base_dict_type` VALUES (215, 0, 'send_status', '发送状态', 0, 0, '邮件/短信发送状态', NULL, '2026-06-25 00:00:00', NULL, '2026-06-25 00:00:00', 0);

INSERT INTO `base_dict_item` VALUES (3310, 0, 'send_status', '待发送',   '0', 1, 0, NULL, '2026-06-25 00:00:00', NULL, '2026-06-25 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (3311, 0, 'send_status', '已发送',   '1', 2, 0, NULL, '2026-06-25 00:00:00', NULL, '2026-06-25 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (3312, 0, 'send_status', '发送失败', '2', 3, 0, NULL, '2026-06-25 00:00:00', NULL, '2026-06-25 00:00:00', 0);

-- ===================== 短信类型字典 =====================
-- dict_type: sms_type

INSERT INTO `base_dict_type` VALUES (216, 0, 'sms_type', '短信类型', 0, 0, '短信发送的业务类型', NULL, '2026-06-25 00:00:00', NULL, '2026-06-25 00:00:00', 0);

INSERT INTO `base_dict_item` VALUES (3320, 0, 'sms_type', '新用户通知', 'NEW_USER_NOTIFY',       1, 0, NULL, '2026-06-25 00:00:00', NULL, '2026-06-25 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (3321, 0, 'sms_type', '重置密码',   'RESET_PASSWORD_NOTIFY', 2, 0, NULL, '2026-06-25 00:00:00', NULL, '2026-06-25 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (3322, 0, 'sms_type', '验证码',     'VERIFICATION_CODE',     3, 0, NULL, '2026-06-25 00:00:00', NULL, '2026-06-25 00:00:00', 0);
INSERT INTO `base_dict_item` VALUES (3323, 0, 'sms_type', '登录通知',   'LOGIN_NOTIFY',          4, 0, NULL, '2026-06-25 00:00:00', NULL, '2026-06-25 00:00:00', 0);
