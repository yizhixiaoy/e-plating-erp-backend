package com.plating.erp.iam.vo;

import com.plating.erp.common.validation.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 角色相关 VO
 * 
 * 校验规则与前端 validation.ts 和数据库约束保持一致
 */
public class RoleVo {
    public record RoleCreateReq(
            @NotNull(message = "租户ID不能为空") Long tenantId,
            Long deptId,
            @NotBlank(message = "角色名称不能为空") 
            @Size(min = ValidationConstants.ROLE_NAME_MIN_LENGTH, max = ValidationConstants.ROLE_NAME_MAX_LENGTH, 
                  message = "角色名称长度为2-32个字符") 
            String roleName,
            @NotBlank(message = "权限字符不能为空") 
            @Size(min = ValidationConstants.ROLE_KEY_MIN_LENGTH, max = ValidationConstants.ROLE_KEY_MAX_LENGTH, 
                  message = "权限字符长度为1-64个字符") 
            @Pattern(regexp = ValidationConstants.ROLE_KEY_REGEX, message = ValidationConstants.ROLE_KEY_MESSAGE) 
            String roleKey,
            @NotNull(message = "数据范围不能为空") 
            @Min(value = 1, message = "数据范围最小为1") 
            @Max(value = 4, message = "数据范围最大为4") 
            Integer dataScope
    ) {
    }

    public record RoleUpdateReq(
            Long tenantId,
            Long deptId,
            @Size(max = ValidationConstants.ROLE_NAME_MAX_LENGTH, 
                  message = "角色名称长度不能超过32个字符") 
            String roleName,
            @Size(max = ValidationConstants.ROLE_KEY_MAX_LENGTH, 
                  message = "权限字符长度不能超过64个字符") 
            @Pattern(regexp = "^$|" + ValidationConstants.ROLE_KEY_REGEX, message = ValidationConstants.ROLE_KEY_MESSAGE) 
            String roleKey,
            @Min(value = 1, message = "数据范围最小为1") 
            @Max(value = 4, message = "数据范围最大为4") 
            Integer dataScope,
            @NotNull(message = "状态不能为空") 
            @Min(value = 0, message = "状态只能为0或1") 
            @Max(value = 1, message = "状态只能为0或1") 
            Integer status
    ) {
    }

    public record RoleMenusReq(
            @NotNull(message = "菜单ID列表不能为空") 
            List<Long> menuIds, 
            @NotNull(message = "数据范围不能为空") 
            @Min(value = 1, message = "数据范围最小为1") 
            @Max(value = 4, message = "数据范围最大为4") 
            Integer dataScope
    ) {
    }
}
