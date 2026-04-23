package com.plating.erp.platform.controller;

import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.common.util.StringUtil;
import com.plating.erp.platform.entity.TenantEntity;
import com.plating.erp.platform.service.TenantService;
import com.plating.erp.platform.vo.TenantListVo;
import com.plating.erp.platform.vo.TenantVo;
import com.plating.erp.platform.vo.TenantOptionsVo;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {
    private static final Logger log = LoggerFactory.getLogger(TenantController.class);
    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @PreAuthorize("@authz.hasPerm('tenant:add')")
    @AuditLog(module = "租户管理", operateType = "CREATE", bizModule = "tenant", fieldName = "tenant_name")
    public ApiResponse<TenantEntity> create(@Valid @RequestBody TenantVo.TenantCreateReq body) {
        TenantEntity entity = new TenantEntity();
        entity.setTenantName(body.tenantName() == null ? "新租户" : body.tenantName());
        entity.setLogoUrl(body.logoUrl() == null ? "" : body.logoUrl());
        entity.setShortCode(body.shortCode() == null ? "demo" : body.shortCode());
        entity.setContactName(body.contactName() == null ? "联系人" : body.contactName());
        entity.setPhone(body.phone() == null ? "13800000000" : body.phone());
        entity.setExpireTime(body.expireTime());
        entity.setStatus(0);
        entity.setDomain(body.domain() == null ? "" : body.domain());
        entity.setWelcomeText(body.welcomeText() == null ? "" : body.welcomeText());
        tenantService.save(entity);
        return ApiResponse.ok(tenantService.getById(entity.getId()));
    }

    @GetMapping("/options")
    @PreAuthorize("@authz.hasPerm('user:add')")
    public ApiResponse<List<TenantOptionsVo>> options() {
        var user = SecurityUtils.currentUser();
        List<TenantOptionsVo> options;
        if (user.isSystem()) {
            // 平台管理员可看到所有租户
            options = tenantService.listAll().stream()
                    .map(t -> new TenantOptionsVo(t.getId(), t.getTenantName(), t.getShortCode()))
                    .toList();
        } else {
            // 租户用户只看到自己租户
            TenantEntity t = tenantService.getById(user.tenantId());
            options = t == null ? List.of() : List.of(new TenantOptionsVo(t.getId(), t.getTenantName(), t.getShortCode()));
        }
        return ApiResponse.ok(options);
    }

    @GetMapping
    @PreAuthorize("@authz.hasPerm('tenant:view')")
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "20") Integer pageSize,
                               @RequestParam(required = false) Integer status,
                               @RequestParam(required = false) String keyword) {
        var user = SecurityUtils.currentUser();
        Long scope = user.isSystem() ? null : user.tenantId();
        var page = tenantService.page(pageNum, pageSize, status, keyword, scope);
        
        // 转换为 VO
        List<TenantListVo> voList = page.records().stream().map(tenant -> new TenantListVo(
                tenant.getId(),
                tenant.getTenantName(),
                tenant.getLogoUrl(),
                tenant.getShortCode(),
                tenant.getContactName(),
                tenant.getPhone(),
                tenant.getExpireTime(),
                tenant.getStatus(),
                tenant.getDomain(),
                tenant.getWelcomeText(),
                tenant.getCreatedBy(),
                tenant.getCreatedAt(),
                tenant.getUpdatedBy(),
                tenant.getUpdatedAt()
        )).toList();
        
        return ApiResponse.ok(new PageResult<>(voList, page.total()));
    }

    @GetMapping("/{tenantId}")
    @PreAuthorize("@authz.hasPerm('tenant:view')")
    public ApiResponse<TenantEntity> detail(@PathVariable Long tenantId) {
        assertTenantScope(tenantId);
        return ApiResponse.ok(tenantService.getById(tenantId));
    }

    @PutMapping("/{tenantId}")
    @PreAuthorize("@authz.hasPerm('tenant:edit')")
    @AuditLog(module = "租户管理", operateType = "UPDATE", bizModule = "tenant", fieldName = "tenant_name")
    public ApiResponse<TenantEntity> update(@PathVariable Long tenantId, @Valid @RequestBody TenantVo.TenantUpdateReq body) {
        assertTenantScope(tenantId);
        TenantEntity t = tenantService.getById(tenantId);
        if (t == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "租户不存在");
        }
        if (body.tenantName() != null) t.setTenantName(body.tenantName());
        if (body.logoUrl() != null) t.setLogoUrl(StringUtil.blankToNull(body.logoUrl()));
        if (body.shortCode() != null) t.setShortCode(body.shortCode());
        if (body.contactName() != null) t.setContactName(body.contactName());
        if (body.phone() != null) t.setPhone(StringUtil.blankToNull(body.phone()));
        if (body.expireTime() != null) t.setExpireTime(body.expireTime());
        if (body.domain() != null) t.setDomain(StringUtil.blankToNull(body.domain()));
        if (body.welcomeText() != null) t.setWelcomeText(StringUtil.blankToNull(body.welcomeText()));
        tenantService.save(t);
        return ApiResponse.ok(tenantService.getById(t.getId()));
    }

    @PatchMapping("/{tenantId}/status")
    @PreAuthorize("@authz.hasPerm('tenant:status')")
    @AuditLog(module = "租户管理", operateType = "STATUS", bizModule = "tenant", fieldName = "status")
    public ApiResponse<TenantEntity> updateStatus(@PathVariable Long tenantId, @Valid @RequestBody TenantVo.TenantStatusReq body) {
        assertTenantScope(tenantId);
        TenantEntity t = tenantService.getById(tenantId);
        if (t == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "租户不存在");
        }
        t.setStatus(body.status() == null ? 0 : body.status());
        tenantService.save(t);
        return ApiResponse.ok(tenantService.getById(t.getId()));
    }

    private void assertTenantScope(Long tenantId) {
        var user = SecurityUtils.currentUser();
        if (!user.isSystem() && !user.tenantId().equals(tenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该租户");
        }
    }

    /**
     * 修改当前用户所在公司信息
     */
    @PutMapping("/me")
    @PreAuthorize("@authz.hasPerm('company:edit')")
    public ApiResponse<?> updateCurrentCompany(@Valid @RequestBody TenantVo.UpdateCompanyReq payload) {
        var user = SecurityUtils.currentUser();
        
        /*if (user.isSystem()) {
            throw new BizException(ErrorCode.FORBIDDEN, "平台用户无权修改租户信息");
        }*/

        Long tenantId = user.tenantId();
        if (tenantId == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户未绑定租户");
        }

        TenantEntity tenant = tenantService.getById(tenantId);
        if (tenant == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "租户不存在");
        }

        if (payload.tenantName() != null) tenant.setTenantName(payload.tenantName());
        if (payload.shortCode() != null) tenant.setShortCode(payload.shortCode());
        if (payload.contactName() != null) tenant.setContactName(payload.contactName());
        if (payload.phone() != null) tenant.setPhone(StringUtil.blankToNull(payload.phone()));
        if (payload.logoUrl() != null) tenant.setLogoUrl(StringUtil.blankToNull(payload.logoUrl()));

        tenantService.save(tenant);
        log.info("公司信息更新成功, tenantId={}, operator={}", tenantId, user.userId());

        return ApiResponse.ok(new TenantVo.CompanyInfoResult(
                tenant.getTenantName(),
                tenant.getShortCode(),
                tenant.getContactName(),
                tenant.getPhone()
        ));
    }
}
