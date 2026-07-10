package com.plating.erp.biz.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 客户公司相关 VO/Req
 *
 * @author Plating ERP Team
 */
public class CustomerVo {

    /**
     * 客户公司详情响应
     */
    public record CustomerDetailVo(
            Long id,
            Long tenantId,
            String customerName,
            String contactPerson,
            String contactPhone,
            String address,
            String remark,
            Long createdBy,
            String createdByName,
            java.time.LocalDateTime createdAt,
            Long updatedBy,
            String updatedByName,
            java.time.LocalDateTime updatedAt
    ) {
    }

    /**
     * 创建客户公司请求
     */
    public record CustomerCreateReq(
            @NotBlank(message = "客户公司名称不能为空")
            @Size(max = 128, message = "客户公司名称长度不能超过128个字符")
            String customerName,
            @Size(max = 64, message = "联系人长度不能超过64个字符")
            String contactPerson,
            @Size(max = 20, message = "联系电话长度不能超过20个字符")
            String contactPhone,
            @Size(max = 256, message = "地址长度不能超过256个字符")
            String address,
            @Size(max = 512, message = "备注长度不能超过512个字符")
            String remark
    ) {
    }

    /**
     * 更新客户公司请求
     */
    public record CustomerUpdateReq(
            @Size(max = 128, message = "客户公司名称长度不能超过128个字符")
            String customerName,
            @Size(max = 64, message = "联系人长度不能超过64个字符")
            String contactPerson,
            @Size(max = 20, message = "联系电话长度不能超过20个字符")
            String contactPhone,
            @Size(max = 256, message = "地址长度不能超过256个字符")
            String address,
            @Size(max = 512, message = "备注长度不能超过512个字符")
            String remark
    ) {
    }
}
