package com.plating.erp.biz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 加工节点版本历史实体
 *
 * @author Plating ERP Team
 */
@Data
@TableName(value = "biz_goods_process_node_version", autoResultMap = true)
public class GoodsNodeVersionEntity {

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
     * 加工节点ID
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
     * 节点状态快照
     */
    private String status;

    /**
     * 原始数量
     */
    private BigDecimal originalQuantity;

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
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 加工完成时间
     */
    private LocalDateTime processedAt;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 修改人ID
     */
    private Long changedBy;

    /**
     * 修改时间
     */
    private LocalDateTime changedAt;

    /**
     * 变更摘要
     */
    private String changeSummary;

    /**
     * 完整节点快照（JSON格式）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object fullSnapshot;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
