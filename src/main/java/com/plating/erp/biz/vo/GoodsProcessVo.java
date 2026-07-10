package com.plating.erp.biz.vo;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 加工节点相关 VO/Req
 *
 * @author Plating ERP Team
 */
public class GoodsProcessVo {

    /**
     * 录入加工数据请求
     */
    public record ProcessSubmitReq(
            @NotNull(message = "节点ID不能为空")
            Long nodeId,
            @NotNull(message = "加工后数量不能为空")
            BigDecimal processedQuantity,
            List<String> afterPhotoUrls,
            String remark,
            @NotNull(message = "操作人ID不能为空")
            Long operatorId,
            String operatorName
    ) {
    }

    /**
     * 节点状态变更请求
     */
    public record NodeStatusChangeReq(
            @NotNull(message = "节点ID不能为空")
            Long nodeId,
            @NotNull(message = "目标状态不能为空")
            String targetStatus,
            @NotNull(message = "操作人ID不能为空")
            Long operatorId,
            String operatorName,
            String remark
    ) {
    }

    /**
     * 节点回退请求
     */
    public record NodeRollbackReq(
            @NotNull(message = "节点ID不能为空")
            Long nodeId,
            @NotNull(message = "操作人ID不能为空")
            Long operatorId,
            String operatorName,
            String reason
    ) {
    }

    /**
     * 加工记录响应
     */
    public record ProcessRecordVo(
            Long id,
            Long nodeId,
            String orderNo,
            Long itemId,
            String itemName,
            Long departmentId,
            String departmentName,
            Integer nodeOrder,
            String status,
            BigDecimal originalQuantity,
            BigDecimal processedQuantity,
            BigDecimal lossQuantity,
            BigDecimal lossRate,
            List<String> beforePhotoUrls,
            List<String> afterPhotoUrls,
            String remark,
            String operatorName,
            java.time.LocalDateTime processedAt
    ) {
    }

    /**
     * 多条件搜索请求
     */
    public record GoodsSearchReq(
            String keyword,
            Long customerId,
            String status,
            Long departmentId,
            Integer page,
            Integer size
    ) {
    }

    /**
     * 图片搜索请求
     */
    public record ImageSearchReq(
            @NotNull(message = "图片URL不能为空")
            String imageUrl,
            Double threshold
    ) {
    }

    /**
     * 搜索结果响应
     */
    public record GoodsSearchResultVo(
            Long orderId,
            String orderNo,
            String customerName,
            String status,
            Integer totalItems,
            java.time.LocalDateTime createdAt
    ) {
    }

    /**
     * 图片搜索匹配结果
     */
    public record ImageSearchResultVo(
            Long itemId,
            String orderNo,
            String itemName,
            Double similarity,
            String thumbnailUrl
    ) {
    }
}
