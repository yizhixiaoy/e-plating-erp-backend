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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
            setExcelResponse(response, "goods_order_" + order.getOrderNo() + ".xlsx");

            ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).build();

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
            log.info("导出Excel成功: orderId={}, orderNo={}", orderId, order.getOrderNo());
        } catch (IOException e) {
            log.error("导出Excel失败: orderId={}", orderId, e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "导出Excel失败");
        }
    }

    @Override
    public void exportPdf(Long orderId, Long tenantId, HttpServletResponse response) {
        // PDF导出使用PDFBox，实现较复杂，暂保留为后续实现
        log.warn("PDF导出功能尚未实现: orderId={}", orderId);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=goods_order_" + orderId + ".pdf");
    }

    @Override
    public void batchExportExcel(Long tenantId, List<String> orderNos, HttpServletResponse response) {
        if (orderNos == null || orderNos.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "开单编号列表不能为空");
        }

        try {
            setExcelResponse(response, "goods_orders_batch.xlsx");

            ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).build();

            // Sheet1: 批量开单基本信息
            WriteSheet sheet1 = EasyExcel.writerSheet(0, "开单信息").head(GoodsExportVo.GoodsOrderExcelDto.class).build();
            List<GoodsExportVo.GoodsOrderExcelDto> orderData = new ArrayList<>();
            for (String orderNo : orderNos) {
                LambdaQueryWrapper<GoodsOrderEntity> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(GoodsOrderEntity::getOrderNo, orderNo)
                       .eq(GoodsOrderEntity::getTenantId, tenantId)
                       .eq(GoodsOrderEntity::getDeleted, 0);
                GoodsOrderEntity order = orderMapper.selectOne(wrapper);
                if (order != null) {
                    orderData.addAll(buildOrderData(order));
                }
            }
            excelWriter.write(orderData, sheet1);

            // Sheet2: 批量货物明细
            WriteSheet sheet2 = EasyExcel.writerSheet(1, "货物明细").head(GoodsExportVo.GoodsItemExcelDto.class).build();
            List<GoodsExportVo.GoodsItemExcelDto> itemData = new ArrayList<>();
            for (String orderNo : orderNos) {
                LambdaQueryWrapper<GoodsOrderEntity> orderWrapper = new LambdaQueryWrapper<>();
                orderWrapper.eq(GoodsOrderEntity::getOrderNo, orderNo)
                            .eq(GoodsOrderEntity::getTenantId, tenantId)
                            .eq(GoodsOrderEntity::getDeleted, 0);
                GoodsOrderEntity order = orderMapper.selectOne(orderWrapper);
                if (order != null) {
                    LambdaQueryWrapper<GoodsOrderItemEntity> itemWrapper = new LambdaQueryWrapper<>();
                    itemWrapper.eq(GoodsOrderItemEntity::getOrderId, order.getId())
                               .eq(GoodsOrderItemEntity::getDeleted, 0);
                    List<GoodsOrderItemEntity> items = itemMapper.selectList(itemWrapper);
                    itemData.addAll(buildItemData(orderNo, items));
                }
            }
            excelWriter.write(itemData, sheet2);

            // Sheet3: 批量加工记录
            WriteSheet sheet3 = EasyExcel.writerSheet(2, "加工记录").head(GoodsExportVo.ProcessRecordExcelDto.class).build();
            List<GoodsExportVo.ProcessRecordExcelDto> processData = new ArrayList<>();
            for (String orderNo : orderNos) {
                LambdaQueryWrapper<GoodsOrderEntity> orderWrapper = new LambdaQueryWrapper<>();
                orderWrapper.eq(GoodsOrderEntity::getOrderNo, orderNo)
                            .eq(GoodsOrderEntity::getTenantId, tenantId)
                            .eq(GoodsOrderEntity::getDeleted, 0);
                GoodsOrderEntity order = orderMapper.selectOne(orderWrapper);
                if (order != null) {
                    LambdaQueryWrapper<GoodsProcessNodeEntity> nodeWrapper = new LambdaQueryWrapper<>();
                    nodeWrapper.eq(GoodsProcessNodeEntity::getOrderId, order.getId())
                               .eq(GoodsProcessNodeEntity::getDeleted, 0)
                               .orderByAsc(GoodsProcessNodeEntity::getItemId)
                               .orderByAsc(GoodsProcessNodeEntity::getNodeOrder);
                    List<GoodsProcessNodeEntity> nodes = nodeMapper.selectList(nodeWrapper);
                    processData.addAll(buildProcessData(nodes));
                }
            }
            excelWriter.write(processData, sheet3);

            excelWriter.finish();
            log.info("批量导出Excel成功: count={}", orderNos.size());
        } catch (IOException e) {
            log.error("批量导出Excel失败", e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "批量导出Excel失败");
        }
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
                order.getStatus(),
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
            dto.setNodeStatus(n.getStatus());
            dto.setOriginalQuantity(n.getOriginalQuantity());
            dto.setProcessedQuantity(n.getProcessedQuantity());
            dto.setLossQuantity(n.getLossQuantity());
            dto.setOperatorName(n.getOperatorName());
            dto.setProcessedAt(n.getProcessedAt() != null ? n.getProcessedAt().format(DATETIME_FMT) : "");
            dto.setRemark(n.getRemark());
            return dto;
        }).collect(Collectors.toList());
    }

    private void setExcelResponse(HttpServletResponse response, String filename) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
    }
}
