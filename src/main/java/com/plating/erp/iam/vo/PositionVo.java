package com.plating.erp.iam.vo;

import com.plating.erp.common.validation.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 岗位相关 VO
 */
public class PositionVo {

    public record PositionCreateReq(
            @NotBlank(message = "岗位名称不能为空")
            @Size(min = 1, max = ValidationConstants.POSITION_NAME_MAX_LENGTH, message = "岗位名称长度不能超过50个字符")
            String positionName,
            Long tenantId,
            Long deptId,
            Integer sortNo,
            Integer status
    ) {
    }

    public record PositionUpdateReq(
            @Size(max = ValidationConstants.POSITION_NAME_MAX_LENGTH, message = "岗位名称长度不能超过50个字符")
            String positionName,
            Long deptId,
            Integer sortNo,
            Integer status
    ) {
    }

    /**
     * 岗位列表/选项响应
     */
    public record PositionListVo(
            Long id,
            Long tenantId,
            String tenantName,
            Long deptId,
            String deptName,
            String positionName,
            Integer sortNo,
            Integer status,
            Long createdBy,
            String createdByName,
            LocalDateTime createdAt,
            Long updatedBy,
            String updatedByName,
            LocalDateTime updatedAt
    ) {
    }
}
