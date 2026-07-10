package com.plating.erp.biz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 货物变更日志实体
 *
 * @author Plating ERP Team
 */
@Data
@TableName(value = "biz_goods_change_log", autoResultMap = true)
public class GoodsChangeLogEntity {

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
     * 开单编号
     */
    private String orderNo;

    /**
     * 节点ID（如有）
     */
    private Long nodeId;

    /**
     * 变更类型：ORDER_CREATE/ORDER_UPDATE/NODE_UPDATE/NODE_STATUS_CHANGE/STATUS_AUTO_UPDATE/ROLLBACK
     */
    private String changeType;

    /**
     * 变更字段名
     */
    private String fieldName;

    /**
     * 旧值（JSON格式）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object oldValue;

    /**
     * 新值（JSON格式）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object newValue;

    /**
     * 操作人ID
     */
    private Long changedBy;

    /**
     * 变更时间
     */
    private LocalDateTime changedAt;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
