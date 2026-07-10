package com.plating.erp.biz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 货物开单明细实体
 *
 * @author Plating ERP Team
 */
@Data
@TableName(value = "biz_goods_order_item", autoResultMap = true)
public class GoodsOrderItemEntity {

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
     * 开单ID，关联 biz_goods_order.id
     */
    private Long orderId;

    /**
     * 货物名称
     */
    private String itemName;

    /**
     * 原始数量
     */
    private BigDecimal quantity;

    /**
     * 计量单位：个/件/kg/米/盒等
     */
    private String unit;

    /**
     * 单价（元）
     */
    private BigDecimal unitPrice;

    /**
     * 总价 = quantity × unit_price
     */
    private BigDecimal totalPrice;

    /**
     * 规格型号
     */
    private String specification;

    /**
     * 材质
     */
    private String material;

    /**
     * 样品照片URL列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> photoUrls;

    /**
     * 货物备注
     */
    private String remark;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新人ID
     */
    private Long updatedBy;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除：0-未删除, 1-已删除
     */
    @TableLogic
    private Integer deleted;
}
