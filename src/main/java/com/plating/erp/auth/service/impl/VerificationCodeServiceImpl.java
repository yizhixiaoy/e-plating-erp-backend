package com.plating.erp.auth.service.impl;

import com.plating.erp.auth.service.VerificationCodeService;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.message.service.SmsService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {
    private final RedisTemplate<String, String> redisTemplate;
    private final SmsService smsService;

    public VerificationCodeServiceImpl(RedisTemplate<String, String> redisTemplate,
                                       SmsService smsService) {
        this.redisTemplate = redisTemplate;
        this.smsService = smsService;
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
        // 忽略大小写比较验证码
        if (!storedCode.equalsIgnoreCase(code)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "验证码错误");
        }
        redisTemplate.delete(key);
    }

    @Override
    public void sendSmsCode(String phone, String scene, Long operatorId,Long tenantId) {
        String key = "sms:code:" + phone + ":" + scene;
        checkSendLimit(phone, scene, "SMS");
        String code = generateCode(key);
        smsService.sendVerificationCodeSms(phone, code, scene, operatorId, tenantId);
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

    /**
     * 生成随机验证码：6位数字和字母组合（大写字母）
     */
    private String generateRandomCode(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        // 字符集：数字0-9 + 大写字母A-Z（排除易混淆字符：0/O, 1/I/l）
        String chars = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
