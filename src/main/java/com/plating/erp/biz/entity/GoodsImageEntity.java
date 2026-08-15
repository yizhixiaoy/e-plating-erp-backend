package com.plating.erp.biz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 货物图片实体
 *
 * @author Plating ERP Team
 */
@Data
@TableName(value = "biz_goods_image", autoResultMap = true)
public class GoodsImageEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 开单ID
     */
    private Long orderId;

    /**
     * 货物明细ID
     */
    private Long itemId;

    /**
     * 加工节点ID
     */
    private Long nodeId;

    /**
     * 加工记录ID
     */
    private Long recordId;

    /**
     * 图片类型：SAMPLE-样品照, BEFORE_PROCESS-加工前, AFTER_PROCESS-加工后
     */
    private String imageType;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * AI提取的图片特征（元数据）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object aiFeatures;

    /**
     * AI特征向量ID，关联AI服务goods_image_feature.id
     */
    private Long aiEmbeddingId;

    /**
     * 逻辑删除：0-未删除, 1-已删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
