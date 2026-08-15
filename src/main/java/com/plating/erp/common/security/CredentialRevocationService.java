package com.plating.erp.common.security;

public interface CredentialRevocationService {
    void revokeCredentialsIssuedBeforeNow(long tenantId, long userId);

    boolean isIssuedBeforeRevocation(long tenantId, long userId, long tokenIssuedAtMillis);
}
