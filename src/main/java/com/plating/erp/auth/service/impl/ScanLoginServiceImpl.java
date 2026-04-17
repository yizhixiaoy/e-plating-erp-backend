package com.plating.erp.auth.service.impl;

import com.plating.erp.auth.service.ScanLoginService;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class ScanLoginServiceImpl implements ScanLoginService {
    private final RedisTemplate<String, String> redisTemplate;

    public ScanLoginServiceImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String generateQrTicket() {
        String qrToken = "qr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        redisTemplate.opsForValue().set("scan:ticket:" + qrToken, "PENDING", Duration.ofMinutes(2));
        return qrToken;
    }

    @Override
    public void confirmLogin(String qrToken, Long userId, Long tenantId, String username) {
        String key = "scan:ticket:" + qrToken;
        String status = redisTemplate.opsForValue().get(key);
        if (status == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "二维码已过期");
        }
        redisTemplate.opsForValue().set(key, "CONFIRMED", Duration.ofMinutes(2));
        redisTemplate.opsForValue().set("scan:user:" + qrToken, userId + ":" + tenantId + ":" + username, Duration.ofMinutes(2));
    }

    @Override
    public String getLoginStatus(String qrToken) {
        String key = "scan:ticket:" + qrToken;
        String status = redisTemplate.opsForValue().get(key);
        return status == null ? "EXPIRED" : status;
    }

    @Override
    public String[] getUserInfo(String qrToken) {
        String key = "scan:user:" + qrToken;
        String userInfo = redisTemplate.opsForValue().get(key);
        if (userInfo == null) {
            return null;
        }
        return userInfo.split(":");
    }

    @Override
    public void cleanup(String qrToken) {
        redisTemplate.delete("scan:ticket:" + qrToken);
        redisTemplate.delete("scan:user:" + qrToken);
    }
}
