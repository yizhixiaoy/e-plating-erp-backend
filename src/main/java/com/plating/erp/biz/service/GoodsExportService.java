package com.plating.erp.biz.service;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 导出Service
 *
 * @author Plating ERP Team
 */
public interface GoodsExportService {

    /**
     * 导出Excel（开单信息 + 货物明细 + 加工记录）
     *
     * @param orderId 开单ID
     * @param tenantId 租户ID
     * @param response HTTP响应
     */
    void exportExcel(Long orderId, Long tenantId, HttpServletResponse response);

    /**
     * 导出PDF
     *
     * @param orderId 开单ID
     * @param tenantId 租户ID
     * @param response HTTP响应
     */
    void exportPdf(Long orderId, Long tenantId, HttpServletResponse response);

    /**
     * 批量导出Excel（按查询条件）
     *
     * @param tenantId 租户ID
     * @param orderIds 开单ID列表
     * @param response HTTP响应
     */
    void batchExportExcel(Long tenantId, List<Long> orderIds, HttpServletResponse response);
}
