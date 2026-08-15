package com.plating.erp.iam.vo;

import com.plating.erp.common.validation.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 部门相关 VO
 */
public class DeptVo {

    public record DeptCreateReq(
            @NotBlank(message = "部门名称不能为空")
            @Size(min = 1, max = ValidationConstants.DEPT_NAME_MAX_LENGTH, message = "部门名称长度不能超过64个字符")
            String deptName,
            Long tenantId,
            Long parentId,
            Long leaderUserId,
            Integer sortNo,
            Integer status
    ) {
    }

    public record DeptUpdateReq(
            @Size(max = ValidationConstants.DEPT_NAME_MAX_LENGTH, message = "部门名称长度不能超过64个字符")
            String deptName,
            Long parentId,
            Long leaderUserId,
            Integer sortNo,
            Integer status
    ) {
    }

    /**
     * 部门列表响应
     */
    public record DeptListVo(
            Long id,
            Long tenantId,
            String tenantName,
            Long parentId,
            String deptName,
            Long leaderUserId,
            String leaderName,
            String leaderUsername,
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
