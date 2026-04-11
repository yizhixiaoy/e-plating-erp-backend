package com.plating.erp.iam.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class UserVo {
    public record UserCreateReq(
            @NotNull(message = "tenantId不能为空") Long tenantId,
            @NotBlank(message = "username不能为空") @Size(min = 2, max = 32, message = "username长度需在2-32") @Pattern(regexp = "^[a-zA-Z0-9:_-]+$", message = "username格式不合法") String username,
            @NotBlank(message = "password不能为空") @Size(min = 6, max = 64, message = "password长度需在6-64") String password,
            @NotBlank(message = "realName不能为空") @Size(min = 2, max = 30, message = "realName长度需在2-30") String realName,
            String avatarUrl,
            Long deptId,
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
            @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", message = "邮箱格式不正确") String email
    ) {
    }

    public record UserUpdateReq(
            @Size(min = 2, max = 32, message = "username长度需在2-32") @Pattern(regexp = "^[a-zA-Z0-9:_-]+$", message = "username格式不合法") String username,
            @Size(min = 2, max = 30, message = "realName长度需在2-30") String realName,
            String avatarUrl,
            Long deptId,
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
            @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", message = "邮箱格式不正确") String email
    ) {
    }

    public record UserStatusReq(@NotNull(message = "status不能为空") @Min(value = 0, message = "status只能为0或1") @Max(value = 1, message = "status只能为0或1") Integer status) {
    }

    public record UserRoleBindReq(
            @NotNull(message = "roleIds不能为空") @Size(min = 1, message = "roleIds至少包含1个角色") List<@NotNull(message = "roleId不能为空") Long> roleIds
    ) {
    }
}
