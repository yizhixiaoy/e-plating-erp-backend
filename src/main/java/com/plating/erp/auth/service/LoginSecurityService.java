package com.plating.erp.auth.service;

public interface LoginSecurityService {
    void checkLoginAttempt(String username, String tenantCode);

    void recordLoginAttempt(String username, String tenantCode, boolean success);

    void checkBruteForce(String ip);

    void recordBruteForceAttempt(String ip, boolean success);
}
