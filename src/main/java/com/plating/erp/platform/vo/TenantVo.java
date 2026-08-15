package com.plating.erp.platform.vo;

import com.plating.erp.common.validation.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 租户相关 VO
 * 
 * 校验规则与前端 validation.ts 和数据库约束保持一致
 */
public class TenantVo {
    public record TenantCreateReq(
            @NotBlank(message = "租户名称不能为空") 
            @Size(min = ValidationConstants.TENANT_NAME_MIN_LENGTH, max = ValidationConstants.TENANT_NAME_MAX_LENGTH, 
                  message = "名称长度为2-64个字符") 
            String tenantName,
            String logoUrl,              // 企业Logo
            @NotBlank(message = "租户简称不能为空") 
            @Size(min = ValidationConstants.SHORT_CODE_MIN_LENGTH, max = ValidationConstants.SHORT_CODE_MAX_LENGTH, 
                  message = "简称长度为2-16个字符") 
            @Pattern(regexp = ValidationConstants.SHORT_CODE_REGEX, message = ValidationConstants.SHORT_CODE_MESSAGE) 
            String shortCode,
            @NotBlank(message = "联系人不能为空") 
            @Size(max = ValidationConstants.CONTACT_NAME_MAX_LENGTH, 
                  message = "联系人长度为2-32个字符") 
            String contactName,
            @NotBlank(message = "手机号不能为空") 
            @Pattern(regexp = ValidationConstants.PHONE_REGEX, message = ValidationConstants.PHONE_MESSAGE) 
            String phone,
            @NotNull(message = "到期时间不能为空") 
            LocalDateTime expireTime,
            @Size(max = ValidationConstants.DOMAIN_MAX_LENGTH, 
                  message = "域名长度不能超过" + ValidationConstants.DOMAIN_MAX_LENGTH + "个字符")
//            @Pattern(regexp = ValidationConstants.DOMAIN_REGEX, message = ValidationConstants.DOMAIN_MESSAGE)
            String domain,               // 自定义域名
            @Size(max = 2000, message = "配置内容不能超过2000字符") 
            String welcomeText           // 租户自定义配置（JSON）
    ) {
    }

    public record TenantUpdateReq(
            @Size(max = ValidationConstants.TENANT_NAME_MAX_LENGTH, 
                  message = "名称长度不能超过64个字符") 
            String tenantName,
            String logoUrl,              // 企业Logo
            @Size(max = ValidationConstants.SHORT_CODE_MAX_LENGTH, 
                  message = "简称长度不能超过16个字符") 
            @Pattern(regexp = "^$|" + ValidationConstants.SHORT_CODE_REGEX, message = ValidationConstants.SHORT_CODE_MESSAGE) 
            String shortCode,
            @Size(max = ValidationConstants.CONTACT_NAME_MAX_LENGTH, 
                  message = "联系人长度不能超过32个字符") 
            String contactName,
            @Pattern(regexp = "^$|" + ValidationConstants.PHONE_REGEX, message = ValidationConstants.PHONE_MESSAGE) 
            String phone,
            LocalDateTime expireTime,
            @Size(max = ValidationConstants.DOMAIN_MAX_LENGTH, 
                  message = "域名长度不能超过" + ValidationConstants.DOMAIN_MAX_LENGTH + "个字符")
//            @Pattern(regexp = "^$|" + ValidationConstants.DOMAIN_REGEX, message = ValidationConstants.DOMAIN_MESSAGE)
            String domain,               // 自定义域名
            @Size(max = 2000, message = "配置内容不能超过2000字符") 
            String welcomeText           // 租户自定义配置（JSON）
    ) {
    }

    public record TenantStatusReq(
            @NotNull(message = "状态不能为空") 
            @Min(value = 0, message = "状态只能为0或1") 
            @Max(value = 1, message = "状态只能为0或1") 
            Integer status
    ) {
    }

    /**
     * 公司信息响应
     */
    public record CompanyInfoResult(
            String tenantName,
            String shortCode,
            String contactName,
            String phone
    ) {
    }

    /**
     * 修改公司信息请求
     */
    public record UpdateCompanyReq(
            @Size(max = ValidationConstants.TENANT_NAME_MAX_LENGTH,
                  message = "名称长度不能超过64个字符")
            String tenantName,
            @Size(max = ValidationConstants.SHORT_CODE_MAX_LENGTH,
                  message = "简称长度不能超过16个字符")
            @Pattern(regexp = "^$|" + ValidationConstants.SHORT_CODE_REGEX, message = ValidationConstants.SHORT_CODE_MESSAGE)
            String shortCode,
            @Size(max = ValidationConstants.CONTACT_NAME_MAX_LENGTH,
                  message = "联系人长度不能超过32个字符")
            String contactName,
            @Pattern(regexp = "^$|" + ValidationConstants.PHONE_REGEX, message = ValidationConstants.PHONE_MESSAGE)
            String phone,
            String logoUrl
    ) {
    }
}
