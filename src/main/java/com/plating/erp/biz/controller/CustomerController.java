package com.plating.erp.biz.controller;

import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.biz.entity.CustomerEntity;
import com.plating.erp.biz.service.CustomerService;
import com.plating.erp.biz.vo.CustomerVo;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户公司控制器
 *
 * @author Plating ERP Team
 */
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * 分页查询客户公司列表
     */
    @GetMapping
    @PreAuthorize("@authz.hasPerm('goods:customer:view')")
    public ApiResponse<PageResult<CustomerEntity>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                                         @RequestParam(required = false) String keyword) {
        var me = SecurityUtils.currentUser();
        var page = customerService.page(me.tenantId(), keyword, pageNum, pageSize);
        return ApiResponse.ok(page);
    }

    /**
     * 查询所有客户公司（用于下拉选择）
     */
    @GetMapping("/options")
    @PreAuthorize("@authz.hasPerm('goods:customer:view')")
    public ApiResponse<List<CustomerEntity>> options() {
        var me = SecurityUtils.currentUser();
        return ApiResponse.ok(customerService.listAll(me.tenantId()));
    }

    /**
     * 根据ID查询客户详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("@authz.hasPerm('goods:customer:view')")
    public ApiResponse<CustomerVo.CustomerDetailVo> detail(@PathVariable Long id) {
        var me = SecurityUtils.currentUser();
        return ApiResponse.ok(customerService.getById(id, me.tenantId()));
    }

    /**
     * 创建客户公司
     */
    @PostMapping
    @PreAuthorize("@authz.hasPerm('goods:customer:add')")
    @AuditLog(module = "客户公司管理", operateType = "CREATE", bizModule = "customer", fieldName = "customerName")
    public ApiResponse<CustomerEntity> create(@Valid @RequestBody CustomerVo.CustomerCreateReq req) {
        var me = SecurityUtils.currentUser();
        return ApiResponse.ok(customerService.create(req, me.tenantId(), me.userId()));
    }

    /**
     * 更新客户公司
     */
    @PutMapping("/{id}")
    @PreAuthorize("@authz.hasPerm('goods:customer:edit')")
    @AuditLog(module = "客户公司管理", operateType = "UPDATE", bizModule = "customer", fieldName = "customerName")
    public ApiResponse<Boolean> update(@PathVariable Long id, @Valid @RequestBody CustomerVo.CustomerUpdateReq req) {
        var me = SecurityUtils.currentUser();
        boolean updated = customerService.update(id, req, me.tenantId(), me.userId());
        if (!updated) {
            throw new BizException(ErrorCode.NOT_FOUND, "客户公司不存在");
        }
        return ApiResponse.ok(updated);
    }

    /**
     * 删除客户公司
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.hasPerm('goods:customer:delete')")
    @AuditLog(module = "客户公司管理", operateType = "DELETE", bizModule = "customer", fieldName = "customerName")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        var me = SecurityUtils.currentUser();
        return ApiResponse.ok(customerService.delete(id, me.tenantId()));
    }
}
