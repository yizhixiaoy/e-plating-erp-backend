package com.plating.erp.audit;

import com.plating.erp.audit.service.LogService;
import com.plating.erp.audit.vo.LogVo;
import com.plating.erp.audit.entity.OperLogEntity;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/logs")
public class LogController {
    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping("/oper")
    @PreAuthorize("@authz.hasPerm('log:view')")
    public ApiResponse<?> operLogs(@RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "20") Integer pageSize,
                                   @RequestParam(required = false) String moduleTitle,
                                   @RequestParam(required = false) Integer status) {
        var me = SecurityUtils.currentUser();
        var page = logService.pageOper(pageNum, pageSize, moduleTitle, status, me.tenantId(), me.isSystem());
        return ApiResponse.ok(new PageResult<>(page.getRecords(), page.getTotal()));
    }

    @GetMapping("/biz")
    @PreAuthorize("@authz.hasPerm('log:view')")
    public ApiResponse<?> bizLogs(@RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "20") Integer pageSize,
                                  @RequestParam(required = false) String bizModule,
                                  @RequestParam(required = false) Long bizId) {
        var me = SecurityUtils.currentUser();
        var page = logService.pageBiz(pageNum, pageSize, bizModule, bizId, me.tenantId(), me.isSystem());
        return ApiResponse.ok(new PageResult<>(page.getRecords(), page.getTotal()));
    }

    @GetMapping("/oper/{logId}")
    @PreAuthorize("@authz.hasPerm('log:view')")
    public ApiResponse<?> operLogDetail(@PathVariable Long logId) {
        OperLogEntity e = logService.getOper(logId);
        if (e == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "日志不存在");
        }
        assertOperLogTenant(e);
        return ApiResponse.ok(e);
    }

    private void assertOperLogTenant(OperLogEntity e) {
        var me = SecurityUtils.currentUser();
        if (me.isSystem()) {
            return;
        }
        if (e.getTenantId() == null || !e.getTenantId().equals(me.tenantId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该日志");
        }
    }

    @PostMapping("/export")
    @PreAuthorize("@authz.hasPerm('log:export')")
    public ApiResponse<CommonResponses.ExportResponse> export(@RequestBody(required = false) LogVo.ExportReq body) {
        return ApiResponse.ok(new CommonResponses.ExportResponse(true, "logs-export.csv", body));
    }
}
