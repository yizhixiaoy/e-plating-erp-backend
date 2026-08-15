-- biz_goods_image 表缺少 deleted 列，但实体类 GoodsImageEntity 使用了 @TableLogic 注解，
-- 导致 MyBatis-Plus 自动追加 WHERE deleted=0 引发 SQL 语法错误。
ALTER TABLE biz_goods_image
    ADD COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除';
