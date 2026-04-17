package com.plating.erp.auth.service;

public interface ScanLoginService {
    String generateQrTicket();

    void confirmLogin(String qrToken, Long userId, Long tenantId, String username);

    String getLoginStatus(String qrToken);

    String[] getUserInfo(String qrToken);

    void cleanup(String qrToken);
}
