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
 * 货物加工节点实体
 *
 * @author Plating ERP Team
 */
@Data
@TableName(value = "biz_goods_process_node", autoResultMap = true)
public class GoodsProcessNodeEntity {

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
     * 开单编号，冗余便于查询
     */
    private String orderNo;

    /**
     * 货物明细ID，关联 biz_goods_order_item.id
     */
    private Long itemId;

    /**
     * 货物名称，冗余便于查询
     */
    private String itemName;

    /**
     * 加工部门ID（关联sys_dept.id）
     */
    private Long departmentId;

    /**
     * 加工部门名称，冗余字段
     */
    private String departmentName;

    /**
     * 节点排序号，决定加工顺序
     */
    private Integer nodeOrder;

    /**
     * 原始加工链顺序（分发时设置1,2,3...，回退新增节点为NULL）
     */
    private Integer originalOrder;

    /**
     * 节点状态：PENDING-待处理, PROCESSING-加工中, PROCESSED-加工完成, COMPLETED-完成
     */
    private String status;

    /**
     * 加工前数量（来自货物明细）
     */
    private BigDecimal originalQuantity;

    /**
     * 加工后数量
     */
    private BigDecimal processedQuantity;

    /**
     * 损耗数量 = original - processed
     */
    private BigDecimal lossQuantity;

    /**
     * 损耗率 = loss / original
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
     * 加工完成时间
     */
    private LocalDateTime processedAt;

    /**
     * 回退次数
     */
    private Integer rollbackCount;

    /**
     * 回退原因
     */
    private String rollbackReason;

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
