package com.plating.erp.biz.controller;

import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.biz.service.GoodsOrderService;
import com.plating.erp.biz.vo.GoodsOrderVo;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 货物开单控制器
 *
 * @author Plating ERP Team
 */
@RestController
@RequestMapping("/api/v1/goods/orders")
public class GoodsOrderController {

    private final GoodsOrderService goodsOrderService;

    public GoodsOrderController(GoodsOrderService goodsOrderService) {
        this.goodsOrderService = goodsOrderService;
    }

    /**
     * 创建货物开单（草稿）
     */
    @PostMapping
    @PreAuthorize("@authz.hasPerm('goods:order:add')")
    @AuditLog(module = "货物开单", operateType = "CREATE", bizModule = "goods_order", fieldName = "orderNo")
    public ApiResponse<GoodsOrderVo.GoodsOrderCreateResp> create(@Valid @RequestBody GoodsOrderVo.GoodsOrderCreateReq req) {
        var me = SecurityUtils.currentUser();
        return ApiResponse.ok(goodsOrderService.createOrder(req, me.tenantId(), me.userId(), me.username()));
    }

    /**
     * 分页查询开单列表
     */
    @GetMapping
    @PreAuthorize("@authz.hasPerm('goods:order:view')")
    public ApiResponse<PageResult<GoodsOrderVo.GoodsOrderListVo>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status) {
        var me = SecurityUtils.currentUser();
        return ApiResponse.ok(goodsOrderService.listOrders(me.tenantId(), keyword, customerId, status, page, size));
    }

    /**
     * 获取开单详情
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("@authz.hasPerm('goods:order:view')")
    public ApiResponse<GoodsOrderVo.GoodsOrderDetailVo> detail(@PathVariable Long orderId) {
        var me = SecurityUtils.currentUser();
        return ApiResponse.ok(goodsOrderService.getOrderDetail(orderId, me.tenantId()));
    }

    /**
     * 提交开单（草稿 → 待分发）
     */
    @PostMapping("/{orderId}/submit")
    @PreAuthorize("@authz.hasPerm('goods:order:submit')")
    @AuditLog(module = "货物开单", operateType = "PUBLISH", bizModule = "goods_order", fieldName = "orderNo")
    public ApiResponse<Void> submit(@PathVariable Long orderId) {
        var me = SecurityUtils.currentUser();
        goodsOrderService.submitOrder(orderId, me.tenantId(), me.userId(), me.username());
        return ApiResponse.ok(null);
    }

    /**
     * 分发开单至加工部门
     */
    @PostMapping("/{orderId}/distribute")
    @PreAuthorize("@authz.hasPerm('goods:order:distribute')")
    @AuditLog(module = "货物开单", operateType = "PUBLISH", bizModule = "goods_order", fieldName = "orderNo")
    public ApiResponse<GoodsOrderVo.GoodsOrderDistributeResp> distribute(@PathVariable Long orderId,
                                                                          @Valid @RequestBody GoodsOrderVo.GoodsOrderDistributeReq req) {
        var me = SecurityUtils.currentUser();
        return ApiResponse.ok(goodsOrderService.distributeOrder(orderId, req, me.tenantId()));
    }

    /**
     * 取消开单
     */
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("@authz.hasPerm('goods:order:cancel')")
    @AuditLog(module = "货物开单", operateType = "REVOKE", bizModule = "goods_order", fieldName = "orderNo")
    public ApiResponse<Void> cancel(@PathVariable Long orderId) {
        var me = SecurityUtils.currentUser();
        goodsOrderService.cancelOrder(orderId, me.tenantId(), me.userId());
        return ApiResponse.ok(null);
    }

    /**
     * 取消分发（待分发 → 草稿）
     */
    @PostMapping("/{orderId}/cancel-distribute")
    @PreAuthorize("@authz.hasPerm('goods:order:cancel')")
    @AuditLog(module = "货物开单", operateType = "REVOKE", bizModule = "goods_order", fieldName = "orderNo")
    public ApiResponse<Void> cancelDistribute(@PathVariable Long orderId) {
        var me = SecurityUtils.currentUser();
        goodsOrderService.cancelDistributeOrder(orderId, me.tenantId(), me.userId());
        return ApiResponse.ok(null);
    }

    /**
     * 重新提交（已取消 → 草稿）
     */
    @PostMapping("/{orderId}/resubmit")
    @PreAuthorize("@authz.hasPerm('goods:order:submit')")
    @AuditLog(module = "货物开单", operateType = "PUBLISH", bizModule = "goods_order", fieldName = "orderNo")
    public ApiResponse<Void> resubmit(@PathVariable Long orderId) {
        var me = SecurityUtils.currentUser();
        goodsOrderService.resubmitOrder(orderId, me.tenantId(), me.userId(), me.username());
        return ApiResponse.ok(null);
    }

    /**
     * 获取开单统计
     */
    @GetMapping("/stats")
    @PreAuthorize("@authz.hasPerm('goods:order:view')")
    public ApiResponse<GoodsOrderService.GoodsOrderStatsVo> stats() {
        var me = SecurityUtils.currentUser();
        return ApiResponse.ok(goodsOrderService.getStats(me.tenantId()));
    }

    /**
     * 更新开单（仅草稿状态可编辑）
     */
    @PutMapping("/{orderId}")
    @PreAuthorize("@authz.hasPerm('goods:order:edit')")
    @AuditLog(module = "货物开单", operateType = "UPDATE", bizModule = "goods_order", fieldName = "orderNo")
    public ApiResponse<Void> update(@PathVariable Long orderId,
                                     @Valid @RequestBody GoodsOrderVo.GoodsOrderUpdateReq req) {
        var me = SecurityUtils.currentUser();
        goodsOrderService.updateOrder(orderId, req, me.tenantId(), me.userId(), me.username());
        return ApiResponse.ok(null);
    }

    /**
     * 删除开单（仅草稿状态可删除）
     */
    @DeleteMapping("/{orderId}")
    @PreAuthorize("@authz.hasPerm('goods:order:delete')")
    @AuditLog(module = "货物开单", operateType = "DELETE", bizModule = "goods_order", fieldName = "orderNo")
    public ApiResponse<Void> delete(@PathVariable Long orderId) {
        var me = SecurityUtils.currentUser();
        goodsOrderService.deleteOrder(orderId, me.tenantId(), me.userId());
        return ApiResponse.ok(null);
    }
}
