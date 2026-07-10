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
 * 开单版本历史实体
 *
 * @author Plating ERP Team
 */
@Data
@TableName(value = "biz_goods_order_version", autoResultMap = true)
public class GoodsOrderVersionEntity {

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
     * 版本号
     */
    private Integer versionNo;

    /**
     * 开单编号，冗余
     */
    private String orderNo;

    /**
     * 客户公司ID
     */
    private Long customerId;

    /**
     * 客户公司名称
     */
    private String customerName;

    /**
     * 开单部门ID
     */
    private Long departmentId;

    /**
     * 开单部门名称
     */
    private String departmentName;

    /**
     * 开单状态快照
     */
    private String status;

    /**
     * 货物明细总数
     */
    private Integer totalItems;

    /**
     * 货物总数量
     */
    private BigDecimal totalQuantity;

    /**
     * 开单备注
     */
    private String remark;

    /**
     * 开单人ID
     */
    private Long operatorId;

    /**
     * 开单人姓名
     */
    private String operatorName;

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
     * 完整开单快照（含items，JSON格式）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object fullSnapshot;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
