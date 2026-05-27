package com.plating.erp.platform.controller;

import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.security.CurrentUser;
import com.plating.erp.iam.service.UserService;
import com.plating.erp.platform.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作台统计接口
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final UserService userService;
    private final TenantService tenantService;

    public DashboardController(UserService userService, TenantService tenantService) {
        this.userService = userService;
        this.tenantService = tenantService;
    }

    /**
     * 获取工作台统计数据（用户总数、租户总数等）
     */
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> stats(CurrentUser user) {
        Map<String, Object> resp = new LinkedHashMap<>();
        try {
            long userTotal = userService.count();
            long tenantCount = tenantService.count();

            // 计算本月新增趋势（本月新增 vs 上月新增的百分比变化）
            LocalDateTime thisMonthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime lastMonthStart = LocalDate.now().minusMonths(1).withDayOfMonth(1).atStartOfDay();

            long thisMonthNewUsers = userService.countSince(thisMonthStart);
            long lastMonthNewUsers = userService.countSince(lastMonthStart) - thisMonthNewUsers;
            long thisMonthNewTenants = tenantService.countSince(thisMonthStart);
            long lastMonthNewTenants = tenantService.countSince(lastMonthStart) - thisMonthNewTenants;

            resp.put("userTotal", userTotal);
            resp.put("tenantCount", tenantCount);
            resp.put("userTrend", calcTrend(thisMonthNewUsers, lastMonthNewUsers));
            resp.put("tenantTrend", calcTrend(thisMonthNewTenants, lastMonthNewTenants));
        } catch (Exception e) {
            log.error("获取工作台统计数据失败", e);
            resp.put("userTotal", 0L);
            resp.put("tenantCount", 0L);
            resp.put("userTrend", null);
            resp.put("tenantTrend", null);
        }
        return ApiResponse.ok(resp);
    }

    /**
     * 计算趋势百分比：本月新增相对于上月新增的变化率
     * 正数表示增长，负数表示下降，0表示持平
     */
    private Integer calcTrend(long thisMonth, long lastMonth) {
        if (lastMonth == 0) {
            return thisMonth > 0 ? 100 : 0;
        }
        return (int) Math.round(((double) (thisMonth - lastMonth) / lastMonth) * 100);
    }
}