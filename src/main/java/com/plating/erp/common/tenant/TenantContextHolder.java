package com.plating.erp.common.tenant;

/**
 * 租户上下文持有者
 *
 * 使用ThreadLocal存储当前请求的租户ID，确保多线程环境下的租户隔离。
 * 通常在请求拦截器或过滤器中设置，在请求结束时清理。
 */
public final class TenantContextHolder {
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    /**
     * 设置当前线程的租户ID
     *
     * @param tenantId 租户ID
     */
    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    /**
     * 获取当前线程的租户ID
     *
     * @return 租户ID，如果未设置则返回null
     */
    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    /**
     * 清除当前线程的租户ID
     *
     * 必须在请求结束时调用，防止线程复用导致的数据污染
     */
    public static void clear() {
        TENANT_ID.remove();
    }
}
