package com.plating.erp.common.security;

import java.util.List;

public record CurrentUser(Long userId, Long tenantId, String username, List<String> roles) {
    public boolean isSystem() {
        return roles != null && roles.contains("system");
    }
}
