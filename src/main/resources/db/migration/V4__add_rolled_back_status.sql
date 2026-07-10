-- 添加已回退状态字典项
INSERT INTO `base_dict_item` (`id`, `tenant_id`, `dict_type`, `dict_label`, `dict_value`, `sort_no`, `status`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted`)
VALUES (1000000000000120, 0, 'goods_node_status', '已回退', 'ROLLED_BACK', 8, 0, NULL, NOW(), NULL, NOW(), 0);
