package com.plating.erp.platform;

import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.platform.entity.TenantEntity;
import com.plating.erp.platform.service.TenantService;
import com.plating.erp.platform.vo.TenantVo;
import com.plating.erp.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {
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
        entity.setAvatarUrl(body.avatarUrl() == null ? "" : body.avatarUrl());
        entity.setShortCode(body.shortCode() == null ? "demo" : body.shortCode());
        entity.setContactName(body.contactName() == null ? "联系人" : body.contactName());
        entity.setPhone(body.phone() == null ? "13800000000" : body.phone());
        entity.setExpireTime(body.expireTime());
        entity.setStatus(0);
        entity.setDomain(body.domain() == null ? "" : body.domain());
        return ApiResponse.ok(tenantService.save(entity));
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
        return ApiResponse.ok(new PageResult<>(page.getRecords(), page.getTotal()));
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
        if (body.avatarUrl() != null) t.setAvatarUrl(body.avatarUrl());
        if (body.shortCode() != null) t.setShortCode(body.shortCode());
        if (body.contactName() != null) t.setContactName(body.contactName());
        if (body.phone() != null) t.setPhone(body.phone());
        if (body.expireTime() != null) t.setExpireTime(body.expireTime());
        if (body.domain() != null) t.setDomain(body.domain());
        return ApiResponse.ok(tenantService.save(t));
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
        return ApiResponse.ok(tenantService.save(t));
    }

    private void assertTenantScope(Long tenantId) {
        var user = SecurityUtils.currentUser();
        if (!user.isSystem() && !user.tenantId().equals(tenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该租户");
        }
    }
}
