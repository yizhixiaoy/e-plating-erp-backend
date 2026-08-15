package com.plating.erp.biz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 货物开单主表实体
 *
 * @author Plating ERP Team
 */
@Data
@TableName(value = "biz_goods_order", autoResultMap = true)
public class GoodsOrderEntity {

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
     * 开单编号，格式：GDyyyyMMddXXXX
     */
    private String orderNo;

    /**
     * 开单日期
     */
    private LocalDate orderDate;

    /**
     * 客户公司ID（关联biz_customer.id）
     */
    private Long customerId;

    /**
     * 客户公司名称，冗余字段便于查询
     */
    private String customerName;

    /**
     * 开单部门ID（关联sys_dept.id）
     */
    private Long departmentId;

    /**
     * 开单部门名称，冗余字段
     */
    private String departmentName;

    /**
     * 状态：DRAFT-草稿, PENDING_DISTRIBUTE-待分发, PROCESSING-加工中, COMPLETED-已完成, CANCELLED-已取消
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
     * 开单人姓名，冗余字段
     */
    private String operatorName;

    /**
     * 提交时间
     */
    private LocalDateTime submittedAt;

    /**
     * 分发时间
     */
    private LocalDateTime distributedAt;

    /**
     * 接货时间（首个节点开始接货时记录）
     */
    private LocalDateTime acceptedAt;

    /**
     * 送货时间（首个节点进入送货状态时记录）
     */
    private LocalDateTime deliveringAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

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
