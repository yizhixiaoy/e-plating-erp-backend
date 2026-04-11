package com.plating.erp.platform.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class TenantVo {
    public record TenantCreateReq(
            @NotBlank(message = "tenantName不能为空") @Size(min = 2, max = 64, message = "tenantName长度需在2-64") String tenantName,
            String avatarUrl,
            @NotBlank(message = "shortCode不能为空") @Size(max = 16, message = "shortCode长度不能超过16") @Pattern(regexp = "^[a-z]+$", message = "shortCode需为小写字母") String shortCode,
            @NotBlank(message = "contactName不能为空") @Size(max = 32, message = "contactName长度不能超过32") String contactName,
            @NotBlank(message = "phone不能为空") @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
            @NotNull(message = "expireTime不能为空") LocalDateTime expireTime,
            String domain
    ) {
    }

    public record TenantUpdateReq(
            @Size(min = 2, max = 64, message = "tenantName长度需在2-64") String tenantName,
            String avatarUrl,
            @Pattern(regexp = "^[a-z]+$", message = "shortCode需为小写字母") String shortCode,
            @Size(max = 32, message = "contactName长度不能超过32") String contactName,
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
            LocalDateTime expireTime,
            String domain
    ) {
    }

    public record TenantStatusReq(@NotNull(message = "status不能为空") @Min(value = 0, message = "status只能为0或1") @Max(value = 1, message = "status只能为0或1") Integer status) {
    }
}
