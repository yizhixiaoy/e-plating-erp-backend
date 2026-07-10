package com.plating.erp.biz.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 货物开单相关 VO/Req
 *
 * @author Plating ERP Team
 */
public class GoodsOrderVo {

    /**
     * 创建开单请求
     */
    public record GoodsOrderCreateReq(
            @NotNull(message = "开单日期不能为空")
            LocalDate orderDate,
            @NotNull(message = "客户公司ID不能为空")
            Long customerId,
            @NotBlank(message = "客户公司名称不能为空")
            @Size(max = 128)
            String customerName,
            @NotNull(message = "开单部门ID不能为空")
            Long departmentId,
            @Size(max = 64)
            String departmentName,
            @Size(max = 512)
            String remark,
            @NotNull(message = "货物明细不能为空")
            List<@Valid GoodsItemReq> items
    ) {
    }

    /**
     * 更新开单请求
     */
    public record GoodsOrderUpdateReq(
            LocalDate orderDate,
            Long customerId,
            @Size(max = 128)
            String customerName,
            Long departmentId,
            @Size(max = 64)
            String departmentName,
            @Size(max = 512)
            String remark,
            List<@Valid GoodsItemReq> items
    ) {
    }

    /**
     * 提交开单请求
     */
    public record GoodsOrderSubmitReq(
            @NotNull(message = "操作人ID不能为空")
            Long operatorId,
            @Size(max = 64)
            String operatorName
    ) {
    }

    /**
     * 分发开单请求——每个货物可独立配置加工步骤
     */
    public record GoodsOrderDistributeReq(
            @NotNull(message = "货物加工步骤列表不能为空")
            List<@Valid ItemDistributeSteps> itemSteps,
            @NotNull(message = "操作人ID不能为空")
            Long operatorId,
            @Size(max = 64)
            String operatorName
    ) {
    }

    /**
     * 单个货物的加工步骤（分发时为每种货物单独指定加工链路）
     */
    public record ItemDistributeSteps(
            @NotNull(message = "货物明细ID不能为空")
            Long itemId,
            @NotNull(message = "加工步骤列表不能为空")
            List<@Valid ProcessStepReq> steps
    ) {
    }

    /**
     * 加工步骤请求（分发时定义加工顺序）
     */
    public record ProcessStepReq(
            @NotNull(message = "部门ID不能为空")
            Long departmentId,
            @Size(max = 64)
            String departmentName
    ) {
    }

    /**
     * 分发开单响应
     */
    public record GoodsOrderDistributeResp(
            Long orderId,
            List<DistributedNodeVo> distributedNodes
    ) {
    }

    /**
     * 分发节点信息
     */
    public record DistributedNodeVo(
            Long nodeId,
            Long departmentId,
            String departmentName,
            Integer nodeOrder,
            String status
    ) {
    }

    /**
     * 创建开单响应
     */
    public record GoodsOrderCreateResp(
            Long orderId,
            String orderNo,
            String status
    ) {
    }

    /**
     * 货物明细请求
     */
    public record GoodsItemReq(
            @NotBlank(message = "货物名称不能为空")
            @Size(max = 128, message = "货物名称长度不能超过128个字符")
            String itemName,
            @NotNull(message = "数量不能为空")
            BigDecimal quantity,
            @Size(max = 16, message = "单位长度不能超过16个字符")
            String unit,
            BigDecimal unitPrice,
            @Size(max = 128, message = "规格长度不能超过128个字符")
            String specification,
            @Size(max = 64, message = "材质长度不能超过64个字符")
            String material,
            List<String> photoUrls,
            @Size(max = 512, message = "备注长度不能超过512个字符")
            String remark
    ) {
    }

    /**
     * 开单列表响应
     */
    public record GoodsOrderListVo(
            Long id,
            String orderNo,
            LocalDate orderDate,
            Long customerId,
            String customerName,
            Long departmentId,
            String departmentName,
            String status,
            Integer totalItems,
            BigDecimal totalQuantity,
            String remark,
            Long operatorId,
            String operatorName,
            java.time.LocalDateTime submittedAt,
            java.time.LocalDateTime distributedAt,
            java.time.LocalDateTime createdAt
    ) {
    }

    /**
     * 开单详情响应（含货物明细和加工节点）
     */
    public record GoodsOrderDetailVo(
            Long id,
            String orderNo,
            LocalDate orderDate,
            Long customerId,
            String customerName,
            Long departmentId,
            String departmentName,
            String status,
            Integer totalItems,
            BigDecimal totalQuantity,
            String remark,
            Long operatorId,
            String operatorName,
            java.time.LocalDateTime submittedAt,
            java.time.LocalDateTime distributedAt,
            java.time.LocalDateTime acceptedAt,
            java.time.LocalDateTime deliveringAt,
            java.time.LocalDateTime completedAt,
            List<GoodsItemDetailVo> items,
            List<GoodsNodeDetailVo> nodes
    ) {
    }

    /**
     * 货物明细详情
     */
    public record GoodsItemDetailVo(
            Long id,
            String itemName,
            BigDecimal quantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            String specification,
            String material,
            List<String> photoUrls,
            String remark
    ) {
    }

    /**
     * 加工节点详情
     */
    public record GoodsNodeDetailVo(
            Long id,
            Long nodeId,
            Long itemId,
            String itemName,
            Long departmentId,
            String departmentName,
            Integer nodeOrder,
            Integer originalOrder,
            String status,
            BigDecimal originalQuantity,
            BigDecimal processedQuantity,
            BigDecimal lossQuantity,
            BigDecimal lossRate,
            String remark,
            String operatorName,
            java.time.LocalDateTime processedAt,
            Integer rollbackCount,
            String rollbackReason
    ) {
    }
}
