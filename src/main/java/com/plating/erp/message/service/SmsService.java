package com.plating.erp.message.service;

public interface SmsService {

    /**
     * 异步发送短信
     * @param receiverPhone 收件人手机号
     * @param smsType 短信类型（如：验证码、通知等）
     * @param content 短信内容/模板参数
     * @param tenantId 租户ID
     * @param receiverUserId 接收人用户ID（可为null）
     * @param operatorId 操作人用户ID（可为null，如登录前发送验证码）
     */
    void sendSmsAsync(String receiverPhone, String smsType, String content,
                      Long tenantId, Long receiverUserId, Long operatorId);

    /**
     * 发送验证码短信
     * @param phone 手机号
     * @param code 验证码
     * @param scene 验证码场景（LOGIN/RESET_PASSWORD/BIND_PHONE等），同时作为短信类型记录
     * @param operatorId 操作人用户ID（可为null，登录前场景无操作人）
     */
    void sendVerificationCodeSms(String phone, String code, String scene, Long operatorId,Long tenantId);
}
