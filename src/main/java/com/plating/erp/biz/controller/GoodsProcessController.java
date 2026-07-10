package com.plating.erp.biz.controller;

import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.biz.service.GoodsProcessService;
import com.plating.erp.biz.vo.GoodsProcessVo;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 货物加工控制器
 *
 * @author Plating ERP Team
 */
@RestController
@RequestMapping("/api/v1/goods/process")
public class GoodsProcessController {

    private final GoodsProcessService goodsProcessService;

    public GoodsProcessController(GoodsProcessService goodsProcessService) {
        this.goodsProcessService = goodsProcessService;
    }

    /**
     * 录入加工数据
     */
    @PostMapping
    @PreAuthorize("@authz.hasPerm('goods:process:submit')")
    @AuditLog(module = "货物加工", operateType = "CREATE", bizModule = "goods_process", fieldName = "orderNo")
    public ApiResponse<GoodsProcessVo.ProcessRecordVo> submit(@Valid @RequestBody GoodsProcessVo.ProcessSubmitReq req) {
        return ApiResponse.ok(goodsProcessService.submitProcess(req));
    }

    /**
     * 变更节点状态
     */
    @PostMapping("/{nodeId}/status")
    @PreAuthorize("@authz.hasPerm('goods:process:status')")
    @AuditLog(module = "货物加工", operateType = "UPDATE", bizModule = "goods_process", fieldName = "status")
    public ApiResponse<Void> changeStatus(@PathVariable Long nodeId,
                                           @Valid @RequestBody GoodsProcessVo.NodeStatusChangeReq req) {
        if (!nodeId.equals(req.nodeId())) {
            throw new com.plating.erp.common.api.BizException(
                    com.plating.erp.common.api.ErrorCode.BAD_REQUEST, "路径中的节点ID与请求体不一致");
        }
        goodsProcessService.changeNodeStatus(req);
        return ApiResponse.ok(null);
    }

    /**
     * 回退节点
     */
    @PostMapping("/{nodeId}/rollback")
    @PreAuthorize("@authz.hasPerm('goods:process:rollback')")
    @AuditLog(module = "货物加工", operateType = "REVOKE", bizModule = "goods_process", fieldName = "orderNo")
    public ApiResponse<Void> rollback(@PathVariable Long nodeId,
                                       @Valid @RequestBody GoodsProcessVo.NodeRollbackReq req) {
        if (!nodeId.equals(req.nodeId())) {
            throw new com.plating.erp.common.api.BizException(
                    com.plating.erp.common.api.ErrorCode.BAD_REQUEST, "路径中的节点ID与请求体不一致");
        }
        goodsProcessService.rollbackNode(req);
        return ApiResponse.ok(null);
    }

    /**
     * 查询开单的所有加工节点
     */
    @GetMapping("/{orderId}/nodes")
    @PreAuthorize("@authz.hasPerm('goods:process:view')")
    public ApiResponse<List<GoodsProcessVo.ProcessRecordVo>> getNodes(@PathVariable Long orderId) {
        var me = SecurityUtils.currentUser();
        return ApiResponse.ok(goodsProcessService.getNodesByOrderId(orderId, me.tenantId()));
    }

    /**
     * 查询开单的加工记录汇总
     */
    @GetMapping("/{orderId}/records")
    @PreAuthorize("@authz.hasPerm('goods:process:view')")
    public ApiResponse<List<GoodsProcessVo.ProcessRecordVo>> getRecords(@PathVariable Long orderId) {
        var me = SecurityUtils.currentUser();
        return ApiResponse.ok(goodsProcessService.getProcessRecordsByOrderId(orderId, me.tenantId()));
    }

    /**
     * 多条件搜索货物
     */
    @GetMapping("/search")
    @PreAuthorize("@authz.hasPerm('goods:process:view')")
    public ApiResponse<PageResult<GoodsProcessVo.GoodsSearchResultVo>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        var me = SecurityUtils.currentUser();
        GoodsProcessVo.GoodsSearchReq req = new GoodsProcessVo.GoodsSearchReq(keyword, customerId, status, null, page, size);
        return ApiResponse.ok(goodsProcessService.searchGoods(req, me.tenantId()));
    }

    /**
     * 图片搜索货物
     */
    @PostMapping("/search/image")
    @PreAuthorize("@authz.hasPerm('goods:process:view')")
    public ApiResponse<List<GoodsProcessVo.ImageSearchResultVo>> searchByImage(
            @RequestParam String imageUrl,
            @RequestParam(required = false, defaultValue = "0.7") Double threshold) {
        var me = SecurityUtils.currentUser();
        return ApiResponse.ok(goodsProcessService.searchByImage(imageUrl, threshold, me.tenantId()));
    }

    /**
     * 批量获取货物明细业务数据（供AI服务调用）
     * 返回 item_id 对应的 order_no, item_name, customer_name
     */
    @GetMapping("/items/business-data")
    @PreAuthorize("@authz.hasPerm('goods:process:view')")
    public ApiResponse<List<Map<String, Object>>> getBusinessData(
            @RequestParam List<Long> itemIds,
            @RequestParam Long tenantId) {
        return ApiResponse.ok(goodsProcessService.getBusinessData(itemIds, tenantId));
    }
}
