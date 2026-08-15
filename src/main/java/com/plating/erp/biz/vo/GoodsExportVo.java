package com.plating.erp.biz.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;

import java.math.BigDecimal;
import java.util.List;

/**
 * 导出相关 DTO
 *
 * @author Plating ERP Team
 */
public class GoodsExportVo {

    /**
     * 开单Excel导出DTO
     */
    public static class GoodsOrderExcelDto {
        @ExcelProperty("开单编号")
        @ColumnWidth(22)
        private String orderNo;

        @ExcelProperty("开单日期")
        @ColumnWidth(14)
        private String orderDate;

        @ExcelProperty("客户公司")
        @ColumnWidth(20)
        private String customerName;

        @ExcelProperty("开单部门")
        @ColumnWidth(14)
        private String departmentName;

        @ExcelProperty("状态")
        @ColumnWidth(12)
        private String status;

        @ExcelProperty("货物项数")
        @ColumnWidth(10)
        private Integer totalItems;

        @ExcelProperty("总数量")
        @ColumnWidth(12)
        private BigDecimal totalQuantity;

        @ExcelProperty("操作人")
        @ColumnWidth(12)
        private String operatorName;

        @ExcelProperty("备注")
        @ColumnWidth(30)
        private String remark;

        public GoodsOrderExcelDto() {}

        public GoodsOrderExcelDto(String orderNo, String orderDate, String customerName,
                                  String departmentName, String status, Integer totalItems,
                                  BigDecimal totalQuantity, String operatorName, String remark) {
            this.orderNo = orderNo;
            this.orderDate = orderDate;
            this.customerName = customerName;
            this.departmentName = departmentName;
            this.status = status;
            this.totalItems = totalItems;
            this.totalQuantity = totalQuantity;
            this.operatorName = operatorName;
            this.remark = remark;
        }

        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public String getOrderDate() { return orderDate; }
        public void setOrderDate(String orderDate) { this.orderDate = orderDate; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getTotalItems() { return totalItems; }
        public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
        public BigDecimal getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(BigDecimal totalQuantity) { this.totalQuantity = totalQuantity; }
        public String getOperatorName() { return operatorName; }
        public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    /**
     * 货物明细Excel导出DTO
     */
    public static class GoodsItemExcelDto {
        @ExcelProperty("开单编号")
        @ColumnWidth(22)
        private String orderNo;

        @ExcelProperty("货物名称")
        @ColumnWidth(20)
        private String itemName;

        @ExcelProperty("数量")
        @ColumnWidth(10)
        private BigDecimal quantity;

        @ExcelProperty("单位")
        @ColumnWidth(8)
        private String unit;

        @ExcelProperty("单价")
        @ColumnWidth(10)
        private BigDecimal unitPrice;

        @ExcelProperty("总价")
        @ColumnWidth(12)
        private BigDecimal totalPrice;

        @ExcelProperty("规格")
        @ColumnWidth(14)
        private String specification;

        @ExcelProperty("材质")
        @ColumnWidth(12)
        private String material;

        @ExcelProperty("照片数")
        @ColumnWidth(8)
        private String photoCount;

        @ExcelProperty("备注")
        @ColumnWidth(30)
        private String remark;

        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public BigDecimal getTotalPrice() { return totalPrice; }
        public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
        public String getSpecification() { return specification; }
        public void setSpecification(String specification) { this.specification = specification; }
        public String getMaterial() { return material; }
        public void setMaterial(String material) { this.material = material; }
        public String getPhotoCount() { return photoCount; }
        public void setPhotoCount(String photoCount) { this.photoCount = photoCount; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    /**
     * 加工记录Excel导出DTO
     */
    public static class ProcessRecordExcelDto {
        @ExcelProperty("开单编号")
        @ColumnWidth(22)
        private String orderNo;

        @ExcelProperty("货物名称")
        @ColumnWidth(20)
        private String itemName;

        @ExcelProperty("加工部门")
        @ColumnWidth(14)
        private String departmentName;

        @ExcelProperty("工序序号")
        @ColumnWidth(10)
        private Integer nodeOrder;

        @ExcelProperty("状态")
        @ColumnWidth(12)
        private String nodeStatus;

        @ExcelProperty("原始数量")
        @ColumnWidth(12)
        private BigDecimal originalQuantity;

        @ExcelProperty("加工后数量")
        @ColumnWidth(12)
        private BigDecimal processedQuantity;

        @ExcelProperty("损耗数量")
        @ColumnWidth(12)
        private BigDecimal lossQuantity;

        @ExcelProperty("操作人")
        @ColumnWidth(12)
        private String operatorName;

        @ExcelProperty("加工时间")
        @ColumnWidth(20)
        private String processedAt;

        @ExcelProperty("备注")
        @ColumnWidth(30)
        private String remark;

        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        public Integer getNodeOrder() { return nodeOrder; }
        public void setNodeOrder(Integer nodeOrder) { this.nodeOrder = nodeOrder; }
        public String getNodeStatus() { return nodeStatus; }
        public void setNodeStatus(String nodeStatus) { this.nodeStatus = nodeStatus; }
        public BigDecimal getOriginalQuantity() { return originalQuantity; }
        public void setOriginalQuantity(BigDecimal originalQuantity) { this.originalQuantity = originalQuantity; }
        public BigDecimal getProcessedQuantity() { return processedQuantity; }
        public void setProcessedQuantity(BigDecimal processedQuantity) { this.processedQuantity = processedQuantity; }
        public BigDecimal getLossQuantity() { return lossQuantity; }
        public void setLossQuantity(BigDecimal lossQuantity) { this.lossQuantity = lossQuantity; }
        public String getOperatorName() { return operatorName; }
        public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
        public String getProcessedAt() { return processedAt; }
        public void setProcessedAt(String processedAt) { this.processedAt = processedAt; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}
