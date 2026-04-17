package com.plating.erp.auth.service.impl;

import com.plating.erp.auth.service.VerificationCodeService;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {
    private final RedisTemplate<String, String> redisTemplate;

    public VerificationCodeServiceImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String generateCode(String key) {
        String code = generateRandomCode(6);
        redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(5));
        return code;
    }

    @Override
    public void validateCode(String key, String code) {
        String storedCode = redisTemplate.opsForValue().get(key);
        if (storedCode == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "验证码已过期");
        }
        if (!storedCode.equals(code)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "验证码错误");
        }
        redisTemplate.delete(key);
    }

    @Override
    public void sendSmsCode(String phone, String scene) {
        String key = "sms:code:" + phone + ":" + scene;
        checkSendLimit(phone, scene, "SMS");
        String code = generateCode(key);
        // TODO: 集成短信发送服务
        System.out.println("发送短信验证码 " + code + " 到 " + phone);
    }

    @Override
    public void sendEmailCode(String email, String scene) {
        String key = "email:code:" + email + ":" + scene;
        checkSendLimit(email, scene, "EMAIL");
        String code = generateCode(key);
        // TODO: 集成邮件发送服务
        System.out.println("发送邮件验证码 " + code + " 到 " + email);
    }

    private void checkSendLimit(String target, String scene, String type) {
        String limitKey = type + ":limit:" + target + ":" + scene;
        String countStr = redisTemplate.opsForValue().get(limitKey);
        int count = countStr == null ? 0 : Integer.parseInt(countStr);
        if (count >= 5) {
            throw new BizException(ErrorCode.BAD_REQUEST, "验证码发送过于频繁，请稍后再试");
        }
        redisTemplate.opsForValue().set(limitKey, String.valueOf(count + 1), Duration.ofHours(1));
    }

    private String generateRandomCode(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
