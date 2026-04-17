package com.plating.erp.common.security;

public interface AuthzCacheService {
    void evictUser(Long tenantId, Long userId);

    void evictTenant(Long tenantId);
}
