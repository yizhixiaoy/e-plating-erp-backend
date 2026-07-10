-- 为加工节点表添加 original_order 字段，用于追踪节点在原始加工链中的顺序位置
-- 分发时设置为 1,2,3...，回退插入的新节点设为 NULL
-- 用于回退时确定"上一个原始部门"，实现 a→b→c→d 回退c变成 a→b→c→b→c→d
ALTER TABLE biz_goods_process_node
    ADD COLUMN original_order int NULL COMMENT '原始加工链顺序（分发时设置，回退新增节点为NULL）';
