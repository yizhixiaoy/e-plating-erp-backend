package com.plating.erp.auth.service;

public interface ScanLoginService {
    /**
     * 生成扫码登录票据和二维码
     * @return [qrToken, qrImageBase64]
     */
    String[] generateQrTicket();

    /**
     * 移动端扫码后调用
     * @param qrToken 二维码票据
     * @param userId 扫码用户ID
     */
    void scanTicket(String qrToken, Long userId);

    /**
     * 确认登录（用户在APP点击确认）
     */
    void confirmLogin(String qrToken, Long userId, Long tenantId, String username);

    /**
     * 获取二维码状态
     * @return PENDING | SCANNED | CONFIRMED | EXPIRED
     */
    String getLoginStatus(String qrToken);

    String[] getUserInfo(String qrToken);

    void cleanup(String qrToken);
}
