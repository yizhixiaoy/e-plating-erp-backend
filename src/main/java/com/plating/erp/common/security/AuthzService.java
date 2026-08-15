package com.plating.erp.common.security;

public interface AuthzService {
    boolean hasRole(String roleKey);

    boolean hasPerm(String perm);
}
