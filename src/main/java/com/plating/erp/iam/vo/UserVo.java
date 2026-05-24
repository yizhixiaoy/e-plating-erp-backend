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
 * 用户相关 VO
 * 
 * 校验规则与前端 validation.ts 和数据库约束保持一致
 */
public class UserVo {
    public record UserCreateReq(
            Long tenantId,
            String username,
            String password,
            @NotBlank(message = "姓名不能为空") 
            @Size(min = ValidationConstants.REAL_NAME_MIN_LENGTH, max = ValidationConstants.REAL_NAME_MAX_LENGTH, 
                  message = "姓名长度为2-30个字符") 
            String realName,
            String avatarUrl,
            Long deptId,
            String position,
            Long leaderUserId,
            @Pattern(regexp = "^$|" + ValidationConstants.PHONE_REGEX, message = ValidationConstants.PHONE_MESSAGE) 
            String phone,
            List<Long> roleIds
    ) {
    }

    public record UserUpdateReq(
            @Size(max = ValidationConstants.USERNAME_MAX_LENGTH, 
                  message = "账号长度不能超过12个字符") 
            @Pattern(regexp = "^$|" + ValidationConstants.USERNAME_REGEX, message = ValidationConstants.USERNAME_MESSAGE) 
            String username,
            @Size(max = ValidationConstants.REAL_NAME_MAX_LENGTH, 
                  message = "姓名长度不能超过30个字符") 
            String realName,
            String password,
            String avatarUrl,
            Long deptId,
            String position,              // 岗位
            Long leaderUserId,            // 直属领导用户ID
            @Pattern(regexp = "^$|" + ValidationConstants.PHONE_REGEX, message = ValidationConstants.PHONE_MESSAGE)
            String phone,
            @Pattern(regexp = "^$|" + ValidationConstants.EMAIL_REGEX, message = ValidationConstants.EMAIL_MESSAGE) 
            String email,
            List<Long> roleIds
    ) {
    }

    public record UserStatusReq(
            @NotNull(message = "状态不能为空") 
            @Min(value = 0, message = "状态只能为0或1") 
            @Max(value = 1, message = "状态只能为0或1") 
            Integer status
    ) {
    }

    public record UserRoleBindReq(
            @NotNull(message = "角色ID列表不能为空") 
            @Size(min = 1, message = "至少选择1个角色") 
            List<@NotNull(message = "角色ID不能为空") Long> roleIds
    ) {
    }
}
