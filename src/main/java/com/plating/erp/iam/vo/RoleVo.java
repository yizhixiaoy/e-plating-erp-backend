package com.plating.erp.iam.vo;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RoleVo {
    public record RoleCreateReq(
            @NotNull(message = "tenantId不能为空") Long tenantId,
            @NotBlank(message = "roleName不能为空") @Size(max = 32, message = "roleName长度不能超过32") String roleName,
            @NotBlank(message = "roleKey不能为空") @Pattern(regexp = "^[a-zA-Z0-9:_]+$", message = "roleKey格式不合法") String roleKey,
            @NotNull(message = "dataScope不能为空") @Min(value = 1, message = "dataScope最小为1") @Max(value = 4, message = "dataScope最大为4") Integer dataScope
    ) {
    }

    public record RoleUpdateReq(
            @NotNull(message = "tenantId不能为空") Long tenantId,
            @NotBlank(message = "roleName不能为空") @Size(max = 32, message = "roleName长度不能超过32") String roleName,
            @NotBlank(message = "roleKey不能为空") @Pattern(regexp = "^[a-zA-Z0-9:_]+$", message = "roleKey格式不合法") String roleKey,
            @NotNull(message = "dataScope不能为空") @Min(value = 1, message = "dataScope最小为1") @Max(value = 4, message = "dataScope最大为4") Integer dataScope,
            @NotNull(message = "status不能为空") @Min(value = 0, message = "status只能为0或1") @Max(value = 1, message = "status只能为0或1") Integer status
    ) {
    }

    public record RoleMenusReq(@NotNull(message = "menuIds不能为空") List<Long> menuIds, @NotNull(message = "dataScope不能为空") @Min(value = 1, message = "dataScope最小为1") @Max(value = 4, message = "dataScope最大为4") Integer dataScope) {
    }
}
