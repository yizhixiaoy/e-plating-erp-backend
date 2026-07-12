package com.plating.erp.biz.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.plating.erp.biz.entity.*;
import com.plating.erp.biz.mapper.*;
import com.plating.erp.biz.service.GoodsExportService;
import com.plating.erp.biz.vo.GoodsExportVo;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.InputStream;

/**
 * 导出Service实现
 * 使用EasyExcel导出Excel
 *
 * @author Plating ERP Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsExportServiceImpl implements GoodsExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GoodsOrderMapper orderMapper;
    private final GoodsOrderItemMapper itemMapper;
    private final GoodsProcessNodeMapper nodeMapper;

    /**
     * 加载支持中文的字体。优先从系统字体路径加载，找不到则回退到 Helvetica
     */
    private PDFont loadChineseFont(PDDocument doc) throws IOException {
        // 常见系统中文字体路径 (macOS / Linux)
        String[] systemFontPaths = {
            "/Library/Fonts/Arial Unicode.ttf",
            "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/noto/NotoSansSC-Regular.ttf"
        };

        for (String path : systemFontPaths) {
            java.io.File fontFile = new java.io.File(path);
            if (fontFile.exists()) {
                try {
                    PDFont font = PDType0Font.load(doc, fontFile);
                    log.info("Loaded Chinese font from: {}", path);
                    return font;
                } catch (IOException e) {
                    log.warn("Failed to load font from {}: {}", path, e.getMessage());
                }
            }
        }

        // classpath 备选
        try (InputStream is = getClass().getResourceAsStream("/fonts/chinese.ttf")) {
            if (is != null) {
                return PDType0Font.load(doc, is);
            }
        }

        log.warn("No Chinese font found, falling back to Helvetica (Chinese will show as ?)");
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    @Override
    public void exportExcel(Long orderId, Long tenantId, HttpServletResponse response) {
        GoodsOrderEntity order = getOrder(orderId, tenantId);

        // 查询货物明细
        LambdaQueryWrapper<GoodsOrderItemEntity> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(GoodsOrderItemEntity::getOrderId, orderId)
                   .eq(GoodsOrderItemEntity::getDeleted, 0);
        List<GoodsOrderItemEntity> items = itemMapper.selectList(itemWrapper);

        // 查询加工节点（按货物分组）
        LambdaQueryWrapper<GoodsProcessNodeEntity> nodeWrapper = new LambdaQueryWrapper<>();
        nodeWrapper.eq(GoodsProcessNodeEntity::getOrderId, orderId)
                   .eq(GoodsProcessNodeEntity::getDeleted, 0)
                   .orderByAsc(GoodsProcessNodeEntity::getItemId)
                   .orderByAsc(GoodsProcessNodeEntity::getNodeOrder);
        List<GoodsProcessNodeEntity> nodes = nodeMapper.selectList(nodeWrapper);

        try {
            // 1. 先生成完整文件到内存（原子性保证）
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ExcelWriter excelWriter = EasyExcel.write(buffer).build();

            // Sheet1: 开单基本信息
            WriteSheet sheet1 = EasyExcel.writerSheet(0, "开单信息").head(GoodsExportVo.GoodsOrderExcelDto.class).build();
            excelWriter.write(buildOrderData(order), sheet1);

            // Sheet2: 货物明细
            WriteSheet sheet2 = EasyExcel.writerSheet(1, "货物明细").head(GoodsExportVo.GoodsItemExcelDto.class).build();
            excelWriter.write(buildItemData(order.getOrderNo(), items), sheet2);

            // Sheet3: 加工记录汇总
            WriteSheet sheet3 = EasyExcel.writerSheet(2, "加工记录").head(GoodsExportVo.ProcessRecordExcelDto.class).build();
            excelWriter.write(buildProcessData(nodes), sheet3);

            excelWriter.finish();

            // 2. 设置响应头（含 Content-Length）后一次性写入
            byte[] fileBytes = buffer.toByteArray();
            setExcelResponse(response, "goods_order_" + order.getOrderNo() + ".xlsx", fileBytes.length);
            response.getOutputStream().write(fileBytes);
            response.getOutputStream().flush();
            log.info("导出Excel成功: orderId={}, orderNo={}, size={}", orderId, order.getOrderNo(), fileBytes.length);
        } catch (IOException e) {
            log.error("导出Excel失败: orderId={}", orderId, e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "导出Excel失败");
        }
    }

    @Override
    public void exportPdf(Long orderId, Long tenantId, HttpServletResponse response) {
        GoodsOrderEntity order = getOrder(orderId, tenantId);

        // 查询货物明细
        LambdaQueryWrapper<GoodsOrderItemEntity> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(GoodsOrderItemEntity::getOrderId, orderId)
                   .eq(GoodsOrderItemEntity::getDeleted, 0);
        List<GoodsOrderItemEntity> items = itemMapper.selectList(itemWrapper);

        // 查询加工节点
        LambdaQueryWrapper<GoodsProcessNodeEntity> nodeWrapper = new LambdaQueryWrapper<>();
        nodeWrapper.eq(GoodsProcessNodeEntity::getOrderId, orderId)
                   .eq(GoodsProcessNodeEntity::getDeleted, 0)
                   .orderByAsc(GoodsProcessNodeEntity::getItemId)
                   .orderByAsc(GoodsProcessNodeEntity::getNodeOrder);
        List<GoodsProcessNodeEntity> nodes = nodeMapper.selectList(nodeWrapper);

        try (PDDocument doc = new PDDocument()) {
            PDFont font = loadChineseFont(doc);
            PDFont fontBold = font; // TTF 字体无独立 bold 变体，使用同一字体
            float margin = 40f;
            float pageHeight = PDRectangle.A4.getHeight();
            float pageWidth = PDRectangle.A4.getWidth();
            float bottomLimit = 50f;

            // === Page 1: Order Info + Items ===
            PDPage page1 = new PDPage(PDRectangle.A4);
            doc.addPage(page1);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page1)) {
                float y = pageHeight - margin;

                // Title
                cs.beginText();
                cs.setFont(fontBold, 16);
                cs.newLineAtOffset(margin, y);
                cs.showText("货物订单: " + nullToEmpty(order.getOrderNo()));
                cs.endText();
                y -= 28;

                // Order details as key-value pairs
                String[][] orderInfo = {
                    {"开单日期", order.getOrderDate() != null ? order.getOrderDate().format(DATE_FMT) : "-"},
                    {"客户", order.getCustomerName() != null ? order.getCustomerName() : "-"},
                    {"部门", order.getDepartmentName() != null ? order.getDepartmentName() : "-"},
                    {"状态", translateOrderStatus(order.getStatus())},
                    {"货物种类", String.valueOf(order.getTotalItems())},
                    {"总数量", String.valueOf(order.getTotalQuantity())},
                    {"操作员", order.getOperatorName() != null ? order.getOperatorName() : "-"},
                    {"备注", order.getRemark() != null ? order.getRemark() : "-"}
                };
                for (String[] row : orderInfo) {
                    cs.beginText();
                    cs.setFont(fontBold, 10);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(nullToEmpty(row[0]) + ":");
                    cs.endText();
                    cs.beginText();
                    cs.setFont(font, 10);
                    cs.newLineAtOffset(margin + 80, y);
                    cs.showText(nullToEmpty(row[1]));
                    cs.endText();
                    y -= 16;
                }

                // Items section header
                y -= 12;
                cs.beginText();
                cs.setFont(fontBold, 13);
                cs.newLineAtOffset(margin, y);
                cs.showText("货物明细");
                cs.endText();
                y -= 18;

                // Table header line
                cs.setLineWidth(0.5f);
                cs.moveTo(margin, y);
                cs.lineTo(pageWidth - margin, y);
                cs.stroke();
                y -= 14;

                // Column positions for items table
                float[] colX = {margin, margin + 30, margin + 160, margin + 210, margin + 250, margin + 310};
                String[] headers = {"#", "名称", "数量", "单位", "单价", "总价"};
                cs.beginText();
                cs.setFont(fontBold, 9);
                cs.newLineAtOffset(colX[0], y);
                cs.showText(headers[0]);
                cs.endText();
                for (int i = 1; i < headers.length; i++) {
                    cs.beginText();
                    cs.newLineAtOffset(colX[i], y);
                    cs.showText(headers[i]);
                    cs.endText();
                }
                y -= 4;
                cs.moveTo(margin, y);
                cs.lineTo(pageWidth - margin, y);
                cs.stroke();
                y -= 13;

                // Item rows
                int idx = 1;
                for (GoodsOrderItemEntity item : items) {
                    if (y < bottomLimit) break; // stop on this page, add more pages later
                    writeItemRow(cs, font, fontBold, idx++, item, colX, y);
                    y -= 13;
                }

                // Process Records section
                y -= 16;
                if (y < bottomLimit + 40) y = bottomLimit + 40;
                cs.beginText();
                cs.setFont(fontBold, 13);
                cs.newLineAtOffset(margin, y);
                cs.showText("加工记录");
                cs.endText();
                y -= 18;

                // Process table header
                float[] ncolX = {margin, margin + 30, margin + 150, margin + 250, margin + 330};
                String[] nHeaders = {"#", "货物", "部门", "状态", "加工数量"};
                cs.moveTo(margin, y + 14);
                cs.lineTo(pageWidth - margin, y + 14);
                cs.stroke();
                cs.beginText();
                cs.setFont(fontBold, 9);
                cs.newLineAtOffset(ncolX[0], y);
                cs.showText(nHeaders[0]);
                cs.endText();
                for (int i = 1; i < nHeaders.length; i++) {
                    cs.beginText();
                    cs.newLineAtOffset(ncolX[i], y);
                    cs.showText(nHeaders[i]);
                    cs.endText();
                }
                y -= 4;
                cs.moveTo(margin, y);
                cs.lineTo(pageWidth - margin, y);
                cs.stroke();
                y -= 13;

                // Process node rows
                int nodeIdx = 1;
                for (GoodsProcessNodeEntity node : nodes) {
                    if (y < bottomLimit) break;
                    writeNodeRow(cs, font, fontBold, nodeIdx++, node, ncolX, y);
                    y -= 13;
                }
            }

            // === Additional pages if items/nodes overflow ===
            // (s for simplicity, single page covers most orders)

            // 1. 先生成完整PDF到内存（原子性保证）
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            doc.save(buffer);

            // 2. 设置响应头（含 Content-Length）后一次性写入
            byte[] fileBytes = buffer.toByteArray();
            String filename = "goods_order_" + order.getOrderNo() + ".pdf";
            response.setContentType("application/pdf");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentLength(fileBytes.length);
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
            response.getOutputStream().write(fileBytes);
            response.getOutputStream().flush();
            log.info("PDF export success: orderId={}, orderNo={}, size={}", orderId, order.getOrderNo(), fileBytes.length);
        } catch (IOException e) {
            log.error("PDF export failed: orderId={}", orderId, e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "PDF导出失败");
        }
    }

    private void writeItemRow(PDPageContentStream cs, PDFont font, PDFont fontBold,
                              int idx, GoodsOrderItemEntity item, float[] colX, float y) throws IOException {
        cs.setFont(font, 9);
        cs.beginText();
        cs.newLineAtOffset(colX[0], y);
        cs.showText(String.valueOf(idx));
        cs.endText();
        cs.beginText();
        cs.newLineAtOffset(colX[1], y);
        cs.showText(nullToEmpty(item.getItemName()));
        cs.endText();
        cs.beginText();
        cs.newLineAtOffset(colX[2], y);
        cs.showText(item.getQuantity() != null ? item.getQuantity().toString() : "");
        cs.endText();
        cs.beginText();
        cs.newLineAtOffset(colX[3], y);
        cs.showText(nullToEmpty(item.getUnit()));
        cs.endText();
        cs.beginText();
        cs.newLineAtOffset(colX[4], y);
        cs.showText(item.getUnitPrice() != null ? item.getUnitPrice().toString() : "");
        cs.endText();
        cs.beginText();
        cs.newLineAtOffset(colX[5], y);
        cs.showText(item.getTotalPrice() != null ? item.getTotalPrice().toString() : "");
        cs.endText();
    }

    private void writeNodeRow(PDPageContentStream cs, PDFont font, PDFont fontBold,
                              int idx, GoodsProcessNodeEntity node, float[] colX, float y) throws IOException {
        cs.setFont(font, 9);
        cs.beginText();
        cs.newLineAtOffset(colX[0], y);
        cs.showText(String.valueOf(idx));
        cs.endText();
        cs.beginText();
        cs.newLineAtOffset(colX[1], y);
        cs.showText(nullToEmpty(node.getItemName()));
        cs.endText();
        cs.beginText();
        cs.newLineAtOffset(colX[2], y);
        cs.showText(nullToEmpty(node.getDepartmentName()));
        cs.endText();
        cs.beginText();
        cs.newLineAtOffset(colX[3], y);
        cs.showText(translateNodeStatus(node.getStatus()));
        cs.endText();
        cs.beginText();
        cs.newLineAtOffset(colX[4], y);
        cs.showText(node.getProcessedQuantity() != null ? node.getProcessedQuantity().toString() : "0");
        cs.endText();
    }

    /**
     * null 安全字符串转换
     */
    private String nullToEmpty(String text) {
        return text != null ? text : "";
    }

    @Override
    public void batchExportExcel(Long tenantId, List<Long> orderIds, HttpServletResponse response) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "开单ID列表不能为空");
        }

        try {
            // 批量查询订单（避免N+1）
            List<GoodsOrderEntity> allOrders = orderMapper.selectBatchIds(orderIds);
            List<GoodsOrderEntity> validOrders = allOrders.stream()
                    .filter(o -> o.getDeleted() == 0 && o.getTenantId().equals(tenantId))
                    .toList();
            List<Long> validOrderIds = validOrders.stream().map(GoodsOrderEntity::getId).toList();

            // 批量查询货物明细
            Map<Long, List<GoodsOrderItemEntity>> itemsByOrderId = new HashMap<>();
            if (!validOrderIds.isEmpty()) {
                LambdaQueryWrapper<GoodsOrderItemEntity> itemWrapper = new LambdaQueryWrapper<>();
                itemWrapper.in(GoodsOrderItemEntity::getOrderId, validOrderIds)
                           .eq(GoodsOrderItemEntity::getDeleted, 0);
                List<GoodsOrderItemEntity> allItems = itemMapper.selectList(itemWrapper);
                itemsByOrderId = allItems.stream()
                        .collect(Collectors.groupingBy(GoodsOrderItemEntity::getOrderId));
            }

            // 批量查询加工节点
            Map<Long, List<GoodsProcessNodeEntity>> nodesByOrderId = new HashMap<>();
            if (!validOrderIds.isEmpty()) {
                LambdaQueryWrapper<GoodsProcessNodeEntity> nodeWrapper = new LambdaQueryWrapper<>();
                nodeWrapper.in(GoodsProcessNodeEntity::getOrderId, validOrderIds)
                           .eq(GoodsProcessNodeEntity::getDeleted, 0)
                           .orderByAsc(GoodsProcessNodeEntity::getItemId)
                           .orderByAsc(GoodsProcessNodeEntity::getNodeOrder);
                List<GoodsProcessNodeEntity> allNodes = nodeMapper.selectList(nodeWrapper);
                nodesByOrderId = allNodes.stream()
                        .collect(Collectors.groupingBy(GoodsProcessNodeEntity::getOrderId));
            }

            // 1. 先生成完整文件到内存（原子性保证）
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ExcelWriter excelWriter = EasyExcel.write(buffer).build();

            // Sheet1: 批量开单基本信息
            WriteSheet sheet1 = EasyExcel.writerSheet(0, "开单信息").head(GoodsExportVo.GoodsOrderExcelDto.class).build();
            List<GoodsExportVo.GoodsOrderExcelDto> orderData = new ArrayList<>();
            for (GoodsOrderEntity order : validOrders) {
                orderData.addAll(buildOrderData(order));
            }
            excelWriter.write(orderData, sheet1);

            // Sheet2: 批量货物明细
            WriteSheet sheet2 = EasyExcel.writerSheet(1, "货物明细").head(GoodsExportVo.GoodsItemExcelDto.class).build();
            List<GoodsExportVo.GoodsItemExcelDto> itemData = new ArrayList<>();
            for (GoodsOrderEntity order : validOrders) {
                List<GoodsOrderItemEntity> items = itemsByOrderId.getOrDefault(order.getId(), List.of());
                itemData.addAll(buildItemData(order.getOrderNo(), items));
            }
            excelWriter.write(itemData, sheet2);

            // Sheet3: 批量加工记录
            WriteSheet sheet3 = EasyExcel.writerSheet(2, "加工记录").head(GoodsExportVo.ProcessRecordExcelDto.class).build();
            List<GoodsExportVo.ProcessRecordExcelDto> processData = new ArrayList<>();
            for (GoodsOrderEntity order : validOrders) {
                List<GoodsProcessNodeEntity> nodes = nodesByOrderId.getOrDefault(order.getId(), List.of());
                processData.addAll(buildProcessData(nodes));
            }
            excelWriter.write(processData, sheet3);

            excelWriter.finish();

            // 2. 设置响应头（含 Content-Length）后一次性写入
            byte[] fileBytes = buffer.toByteArray();
            setExcelResponse(response, "goods_orders_batch.xlsx", fileBytes.length);
            response.getOutputStream().write(fileBytes);
            response.getOutputStream().flush();
            log.info("批量导出Excel成功: count={}, size={}", validOrders.size(), fileBytes.length);
        } catch (IOException e) {
            log.error("批量导出Excel失败", e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "批量导出Excel失败");
        }
    }

    // ========== 状态翻译 ==========

    private static final Map<String, String> ORDER_STATUS_MAP = Map.of(
            "DRAFT", "草稿",
            "PENDING_DISTRIBUTE", "待分发",
            "PROCESSING", "加工中",
            "COMPLETED", "已完成",
            "CANCELLED", "已取消"
    );

    private static final Map<String, String> NODE_STATUS_MAP = Map.of(
            "PENDING", "待处理",
            "ACCEPTING", "开始接货",
            "PROCESSING", "加工中",
            "PROCESSED", "加工完成",
            "DELIVERING", "送货中",
            "DRIVER_DELIVERED", "司机送达",
            "COMPLETED", "已完成",
            "ROLLED_BACK", "已回退"
    );

    private String translateOrderStatus(String status) {
        return ORDER_STATUS_MAP.getOrDefault(status, status);
    }

    private String translateNodeStatus(String status) {
        return NODE_STATUS_MAP.getOrDefault(status, status);
    }

    // ========== 私有方法 ==========

    private GoodsOrderEntity getOrder(Long orderId, Long tenantId) {
        GoodsOrderEntity order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "开单不存在");
        }
        if (!order.getTenantId().equals(tenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该开单");
        }
        return order;
    }

    private List<GoodsExportVo.GoodsOrderExcelDto> buildOrderData(GoodsOrderEntity order) {
        GoodsExportVo.GoodsOrderExcelDto dto = new GoodsExportVo.GoodsOrderExcelDto(
                order.getOrderNo(),
                order.getOrderDate() != null ? order.getOrderDate().format(DATE_FMT) : "",
                order.getCustomerName(),
                order.getDepartmentName(),
                translateOrderStatus(order.getStatus()),
                order.getTotalItems(),
                order.getTotalQuantity(),
                order.getOperatorName(),
                order.getRemark()
        );
        return List.of(dto);
    }

    private List<GoodsExportVo.GoodsItemExcelDto> buildItemData(String orderNo, List<GoodsOrderItemEntity> items) {
        return items.stream().map(i -> {
            GoodsExportVo.GoodsItemExcelDto dto = new GoodsExportVo.GoodsItemExcelDto();
            dto.setOrderNo(orderNo);
            dto.setItemName(i.getItemName());
            dto.setQuantity(i.getQuantity());
            dto.setUnit(i.getUnit());
            dto.setUnitPrice(i.getUnitPrice());
            dto.setTotalPrice(i.getTotalPrice());
            dto.setSpecification(i.getSpecification());
            dto.setMaterial(i.getMaterial());
            dto.setPhotoCount(i.getPhotoUrls() != null ? String.valueOf(i.getPhotoUrls().size()) : "0");
            dto.setRemark(i.getRemark());
            return dto;
        }).collect(Collectors.toList());
    }

    private List<GoodsExportVo.ProcessRecordExcelDto> buildProcessData(List<GoodsProcessNodeEntity> nodes) {
        return nodes.stream().map(n -> {
            GoodsExportVo.ProcessRecordExcelDto dto = new GoodsExportVo.ProcessRecordExcelDto();
            dto.setOrderNo(n.getOrderNo());
            dto.setItemName(n.getItemName());
            dto.setDepartmentName(n.getDepartmentName());
            dto.setNodeOrder(n.getNodeOrder());
            dto.setNodeStatus(translateNodeStatus(n.getStatus()));
            dto.setOriginalQuantity(n.getOriginalQuantity());
            dto.setProcessedQuantity(n.getProcessedQuantity());
            dto.setLossQuantity(n.getLossQuantity());
            dto.setOperatorName(n.getOperatorName());
            dto.setProcessedAt(n.getProcessedAt() != null ? n.getProcessedAt().format(DATETIME_FMT) : "");
            dto.setRemark(n.getRemark());
            return dto;
        }).collect(Collectors.toList());
    }

    private void setExcelResponse(HttpServletResponse response, String filename, int contentLength) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentLength(contentLength);
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
    }
}
