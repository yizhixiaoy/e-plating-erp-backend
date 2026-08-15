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
 * 货物加工记录实体
 *
 * @author Plating ERP Team
 */
@Data
@TableName(value = "biz_goods_process_record", autoResultMap = true)
public class GoodsProcessRecordEntity {

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
     * 加工节点ID，关联 biz_goods_process_node.id
     */
    private Long nodeId;

    /**
     * 开单ID
     */
    private Long orderId;

    /**
     * 开单编号
     */
    private String orderNo;

    /**
     * 货物明细ID
     */
    private Long itemId;

    /**
     * 货物名称
     */
    private String itemName;

    /**
     * 加工部门ID
     */
    private Long departmentId;

    /**
     * 加工部门名称
     */
    private String departmentName;

    /**
     * 节点排序
     */
    private Integer nodeOrder;

    /**
     * 加工前照片URL列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> beforePhotoUrls;

    /**
     * 加工后照片URL列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> afterPhotoUrls;

    /**
     * 加工后数量
     */
    private BigDecimal processedQuantity;

    /**
     * 损耗数量
     */
    private BigDecimal lossQuantity;

    /**
     * 损耗率
     */
    private BigDecimal lossRate;

    /**
     * 加工备注
     */
    private String remark;

    /**
     * 加工操作人ID
     */
    private Long operatorId;

    /**
     * 加工操作人姓名
     */
    private String operatorName;

    /**
     * 加工时间
     */
    private LocalDateTime processedAt;

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
