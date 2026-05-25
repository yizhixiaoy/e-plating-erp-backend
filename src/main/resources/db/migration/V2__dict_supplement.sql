-- =====================================================
-- V2: 补充字典数据 & 修复 notice_type 启用状态
--     - 租户状态 & 菜单显示/隐藏
--     - notice_type 字典类型及项 status=1 → 0
-- =====================================================

-- 字典类型
INSERT INTO `base_dict_type` (`id`, `tenant_id`, `dict_type`, `dict_name`, `status`, `sort_no`, `remark`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted`) VALUES
(212, 0, 'sys_tenant_status', '租户状态', 0, 0, '0=正常 1=冻结', NULL, NOW(), NULL, NOW(), 0),
(213, 0, 'sys_show_hide', '显示隐藏', 0, 0, '0=隐藏 1=显示', NULL, NOW(), NULL, NOW(), 0);

-- 字典项
INSERT INTO `base_dict_item` (`id`, `tenant_id`, `dict_type`, `dict_label`, `dict_value`, `sort_no`, `status`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted`) VALUES
(356, 0, 'sys_tenant_status', '正常', '0', 1, 0, NULL, NOW(), NULL, NOW(), 0),
(357, 0, 'sys_tenant_status', '冻结', '1', 2, 0, NULL, NOW(), NULL, NOW(), 0),
(358, 0, 'sys_show_hide', '隐藏', '0', 1, 0, NULL, NOW(), NULL, NOW(), 0),
(359, 0, 'sys_show_hide', '显示', '1', 2, 0, NULL, NOW(), NULL, NOW(), 0);

-- 修复 notice_type 字典类型及所有项 status=1 → 0（原为停用状态导致前端下拉框无数据）
UPDATE `base_dict_type` SET `status` = 0 WHERE `dict_type` = 'notice_type' AND `status` = 1;
UPDATE `base_dict_item` SET `status` = 0 WHERE `dict_type` = 'notice_type' AND `status` = 1;
