package com.plating.erp.biz.controller;

import com.plating.erp.biz.service.GoodsVersionService;
import com.plating.erp.biz.vo.GoodsVersionVo;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 货物版本历史控制器
 *
 * @author Plating ERP Team
 */
@RestController
@RequestMapping("/api/v1/goods/versions")
public class GoodsVersionController {

    private final GoodsVersionService goodsVersionService;

    public GoodsVersionController(GoodsVersionService goodsVersionService) {
        this.goodsVersionService = goodsVersionService;
    }

    /**
     * 获取开单版本历史
     */
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("@authz.hasPerm('goods:order:view')")
    public ApiResponse<List<GoodsVersionVo.OrderVersionHistoryVo>> getOrderVersions(@PathVariable Long orderId) {
        var me = SecurityUtils.currentUser();
        return ApiResponse.ok(goodsVersionService.getOrderVersions(orderId, me.tenantId()));
    }

    /**
     * 获取节点版本历史
     */
    @GetMapping("/nodes/{nodeId}")
    @PreAuthorize("@authz.hasPerm('goods:process:view')")
    public ApiResponse<List<GoodsVersionVo.NodeVersionHistoryVo>> getNodeVersions(@PathVariable Long nodeId) {
        var me = SecurityUtils.currentUser();
        return ApiResponse.ok(goodsVersionService.getNodeVersions(nodeId, me.tenantId()));
    }

    /**
     * 获取变更日志
     */
    @GetMapping("/orders/{orderId}/logs")
    @PreAuthorize("@authz.hasPerm('goods:order:view')")
    public ApiResponse<List<GoodsVersionVo.ChangeLogVo>> getChangeLogs(@PathVariable Long orderId) {
        var me = SecurityUtils.currentUser();
        return ApiResponse.ok(goodsVersionService.getChangeLogs(orderId, me.tenantId()));
    }

    /**
     * 获取节点变更日志
     */
    @GetMapping("/nodes/{nodeId}/logs")
    @PreAuthorize("@authz.hasPerm('goods:process:view')")
    public ApiResponse<List<GoodsVersionVo.ChangeLogVo>> getChangeLogsByNode(@PathVariable Long nodeId) {
        var me = SecurityUtils.currentUser();
        return ApiResponse.ok(goodsVersionService.getChangeLogsByNode(nodeId, me.tenantId()));
    }
}
