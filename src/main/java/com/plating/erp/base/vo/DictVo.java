package com.plating.erp.base.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 字典相关 VO
 */
public class DictVo {

    // ==================== 字典类型 ====================

    /**
     * 字典类型列表响应
     */
    public record DictTypeListVo(
            Long id,
            String dictType,
            String dictName,
            Integer status,
            String remark,
            Long tenantId,
            Boolean system,        // 是否为系统字典（tenantId=0）
            Boolean editable,      // 当前用户是否可编辑
            Long createdBy,
            String createdByName,
            LocalDateTime createdAt,
            Long updatedBy,
            String updatedByName,
            LocalDateTime updatedAt
    ) {
    }

    /**
     * 新增字典类型请求
     */
    public record DictTypeCreateReq(
            @NotBlank(message = "字典类型编码不能为空")
            @Size(max = 100, message = "字典类型编码长度不能超过100个字符")
            String dictType,
            @NotBlank(message = "字典类型名称不能为空")
            @Size(max = 100, message = "字典类型名称长度不能超过100个字符")
            String dictName,
            String remark
    ) {
    }

    /**
     * 编辑字典类型请求
     */
    public record DictTypeUpdateReq(
            @Size(max = 100, message = "字典类型名称长度不能超过100个字符")
            String dictName,
            Integer status,
            String remark
    ) {
    }

    // ==================== 字典项 ====================

    /**
     * 字典项列表响应
     */
    public record DictItemListVo(
            Long id,
            String dictType,
            String dictLabel,
            String dictValue,
            Integer sortNo,
            Integer status,
            Long tenantId,
            Boolean system,
            Boolean editable,
            Long createdBy,
            String createdByName,
            LocalDateTime createdAt,
            Long updatedBy,
            String updatedByName,
            LocalDateTime updatedAt
    ) {
    }

    /**
     * 新增字典项请求
     */
    public record DictItemCreateReq(
            @NotBlank(message = "字典类型不能为空")
            String dictType,
            @NotBlank(message = "字典标签不能为空")
            @Size(max = 100, message = "字典标签长度不能超过100个字符")
            String dictLabel,
            @NotBlank(message = "字典键值不能为空")
            @Size(max = 100, message = "字典键值长度不能超过100个字符")
            String dictValue,
            Integer sortNo
    ) {
    }

    /**
     * 编辑字典项请求
     */
    public record DictItemUpdateReq(
            @Size(max = 100, message = "字典标签长度不能超过100个字符")
            String dictLabel,
            @Size(max = 100, message = "字典键值长度不能超过100个字符")
            String dictValue,
            Integer sortNo,
            Integer status
    ) {
    }

    /**
     * 字典数据查询响应（供业务模块下拉选择使用）
     */
    public record DictDataVo(
            String dictLabel,
            String dictValue
    ) {
    }
}