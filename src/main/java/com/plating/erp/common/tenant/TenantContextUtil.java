package com.plating.erp.common.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * 租户上下文工具类
 * 
 * 用于在异步任务、定时任务等场景中安全地传递和管理租户上下文
 * 
 * 使用场景：
 * 1. 异步任务中需要访问租户数据
 * 2. 定时任务按租户分组处理数据
 * 3. 跨线程传递租户上下文
 * 
 * 示例：
 * <pre>
 * // 场景1：异步任务
 * CompletableFuture.runAsync(
 *     TenantContextUtil.withTenant(tenantId, () -> {
 *         // 业务逻辑，自动设置和清理租户上下文
 *         userService.list();
 *     })
 * );
 * 
 * // 场景2：定时任务按租户处理
 * for (Long tenantId : tenantIds) {
 *     TenantContextUtil.runWithTenant(tenantId, () -> {
 *         generateReport(tenantId);
 *     });
 * }
 * </pre>
 */
public final class TenantContextUtil {
    
    private static final Logger log = LoggerFactory.getLogger(TenantContextUtil.class);
    
    private TenantContextUtil() {
        // 防止实例化
    }
    
    /**
     * 在指定租户上下文中执行任务（无返回值）
     * 
     * @param tenantId 租户ID
     * @param runnable 任务
     */
    public static void runWithTenant(Long tenantId, Runnable runnable) {
        Long previousTenantId = TenantContextHolder.getTenantId();
        try {
            TenantContextHolder.setTenantId(tenantId);
            log.debug("设置租户上下文, tenantId={}", tenantId);
            runnable.run();
        } finally {
            if (previousTenantId != null) {
                TenantContextHolder.setTenantId(previousTenantId);
            } else {
                TenantContextHolder.clear();
            }
            log.debug("恢复租户上下文, tenantId={}", previousTenantId);
        }
    }
    
    /**
     * 在指定租户上下文中执行任务（有返回值）
     * 
     * @param tenantId 租户ID
     * @param supplier 任务
     * @param <T> 返回值类型
     * @return 任务执行结果
     */
    public static <T> T callWithTenant(Long tenantId, Supplier<T> supplier) {
        Long previousTenantId = TenantContextHolder.getTenantId();
        try {
            TenantContextHolder.setTenantId(tenantId);
            log.debug("设置租户上下文, tenantId={}", tenantId);
            return supplier.get();
        } finally {
            if (previousTenantId != null) {
                TenantContextHolder.setTenantId(previousTenantId);
            } else {
                TenantContextHolder.clear();
            }
            log.debug("恢复租户上下文, tenantId={}", previousTenantId);
        }
    }
    
    /**
     * 包装 Runnable，使其在指定租户上下文中执行
     * 
     * 使用场景：异步任务、线程池
     * 
     * @param tenantId 租户ID
     * @param runnable 任务
     * @return 包装后的 Runnable
     */
    public static Runnable withTenant(Long tenantId, Runnable runnable) {
        return () -> runWithTenant(tenantId, runnable);
    }
    
    /**
     * 包装 Callable，使其在指定租户上下文中执行
     * 
     * 使用场景：异步任务、线程池
     * 
     * @param tenantId 租户ID
     * @param callable 任务
     * @param <T> 返回值类型
     * @return 包装后的 Callable
     */
    public static <T> Callable<T> withTenant(Long tenantId, Callable<T> callable) {
        return () -> callWithTenant(tenantId, () -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * 清理当前线程的租户上下文
     * 
     * 注意：一般不需要手动调用，runWithTenant/callWithTenant 会自动清理
     */
    public static void clear() {
        TenantContextHolder.clear();
        log.debug("清理租户上下文");
    }
}
