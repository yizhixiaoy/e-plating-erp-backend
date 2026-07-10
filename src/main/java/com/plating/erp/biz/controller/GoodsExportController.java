package com.plating.erp.biz.controller;

import com.plating.erp.biz.service.GoodsExportService;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.security.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 货物导出控制器
 *
 * @author Plating ERP Team
 */
@RestController
@RequestMapping("/api/v1/goods/export")
public class GoodsExportController {

    private final GoodsExportService goodsExportService;

    public GoodsExportController(GoodsExportService goodsExportService) {
        this.goodsExportService = goodsExportService;
    }

    /**
     * 导出Excel
     */
    @GetMapping("/{orderId}/excel")
    @PreAuthorize("@authz.hasPerm('goods:order:export')")
    public void exportExcel(@PathVariable Long orderId, HttpServletResponse response) {
        var me = SecurityUtils.currentUser();
        goodsExportService.exportExcel(orderId, me.tenantId(), response);
    }

    /**
     * 导出PDF
     */
    @GetMapping("/{orderId}/pdf")
    @PreAuthorize("@authz.hasPerm('goods:order:export')")
    public void exportPdf(@PathVariable Long orderId, HttpServletResponse response) {
        var me = SecurityUtils.currentUser();
        goodsExportService.exportPdf(orderId, me.tenantId(), response);
    }

    /**
     * 批量导出Excel
     */
    @PostMapping("/batch/excel")
    @PreAuthorize("@authz.hasPerm('goods:order:export')")
    public void batchExportExcel(@RequestBody List<String> orderNos, HttpServletResponse response) {
        var me = SecurityUtils.currentUser();
        goodsExportService.batchExportExcel(me.tenantId(), orderNos, response);
    }
}
