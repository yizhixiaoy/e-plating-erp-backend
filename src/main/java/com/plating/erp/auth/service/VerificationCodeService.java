package com.plating.erp.auth.service;

public interface VerificationCodeService {
    String generateCode(String key);

    void validateCode(String key, String code);

    void sendSmsCode(String phone, String scene);

    void sendEmailCode(String email, String scene);
}
