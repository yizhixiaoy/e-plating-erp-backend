-- =====================================================
-- V3: 待办撤回状态 & 邮件模板补充
--     - todo_status 新增"已撤回"状态(4)
-- =====================================================

-- 字典项：待办状态 - 已撤回
INSERT INTO `base_dict_item` (`id`, `tenant_id`, `dict_type`, `dict_label`, `dict_value`, `sort_no`, `status`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted`) VALUES
(360, 0, 'todo_status', '已撤回', '4', 5, 0, NULL, NOW(), NULL, NOW(), 0);
