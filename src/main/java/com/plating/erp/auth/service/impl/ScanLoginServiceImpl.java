package com.plating.erp.auth.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.plating.erp.auth.service.ScanLoginService;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 扫码登录服务实现
 * 
 * Token 维护方案：
 * 1. Redis 存储，设置 TTL 自动过期（2分钟）
 * 2. 状态流转：PENDING → SCANNED → CONFIRMED → EXPIRED
 * 3. Redis Key 自动清理，无需手动维护
 * 4. 扫码后刷新 TTL，避免用户操作超时
 */
@Slf4j
@Service
public class ScanLoginServiceImpl implements ScanLoginService {
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.qr-login.baseUrl:https://erp.plating.com}")
    private String qrBaseUrl;

    /** 二维码有效期（分钟） */
    private static final int QR_EXPIRE_MINUTES = 2;
    
    /** Redis Key 前缀 */
    private static final String TICKET_KEY_PREFIX = "scan:ticket:";
    private static final String USER_KEY_PREFIX = "scan:user:";

    public ScanLoginServiceImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String[] generateQrTicket() {
        String qrToken = "qr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String ticketKey = TICKET_KEY_PREFIX + qrToken;
        
        // 存储二维码状态，设置 2 分钟过期
        redisTemplate.opsForValue().set(ticketKey, "PENDING", Duration.ofMinutes(QR_EXPIRE_MINUTES));
        
        log.info("生成扫码登录二维码, qrToken={}, 有效期={}分钟", qrToken, QR_EXPIRE_MINUTES);
        
        // 生成二维码内容（移动端扫码后打开的H5页面URL）
        // 移动端访问此页面，显示登录确认界面，用户确认后调用 /scan 和 /scan-confirm 接口
        String qrContent = qrBaseUrl + "/mobile/scan-confirm?ticket=" + qrToken;
        
        // 生成二维码图片（Base64）
        String qrImageBase64 = generateQrCode(qrContent, 200, 200);
        
        return new String[]{qrToken, qrImageBase64};
    }

    /**
     * 扫码（移动端扫码后调用）
     */
    @Override
    public void scanTicket(String qrToken, Long userId) {
        String ticketKey = TICKET_KEY_PREFIX + qrToken;
        String status = redisTemplate.opsForValue().get(ticketKey);
        
        if (status == null) {
            log.warn("二维码已过期, qrToken={}", qrToken);
            throw new BizException(ErrorCode.BAD_REQUEST, "二维码已过期，请刷新");
        }
        
        if (!"PENDING".equals(status)) {
            log.warn("二维码状态异常, qrToken={}, status={}", qrToken, status);
            throw new BizException(ErrorCode.BAD_REQUEST, "二维码已被使用");
        }
        
        // 更新状态为已扫码，不刷新TTL（保持与前端倒计时一致）
        Long remainingTtl = redisTemplate.getExpire(ticketKey, java.util.concurrent.TimeUnit.SECONDS);
        if (remainingTtl != null && remainingTtl > 0) {
            redisTemplate.opsForValue().set(ticketKey, "SCANNED", java.time.Duration.ofSeconds(remainingTtl));
            redisTemplate.opsForValue().set(USER_KEY_PREFIX + qrToken, String.valueOf(userId), 
                    java.time.Duration.ofSeconds(remainingTtl));
        } else {
            throw new BizException(ErrorCode.BAD_REQUEST, "二维码已过期");
        }
        
        log.info("用户扫码成功, qrToken={}, userId={}, 剩余时间={}秒", qrToken, userId, remainingTtl);
    }

    @Override
    public void confirmLogin(String qrToken, Long userId, Long tenantId, String username) {
        String ticketKey = TICKET_KEY_PREFIX + qrToken;
        String status = redisTemplate.opsForValue().get(ticketKey);
        
        if (status == null) {
            log.warn("二维码已过期, qrToken={}", qrToken);
            throw new BizException(ErrorCode.BAD_REQUEST, "二维码已过期");
        }
        
        if (!"SCANNED".equals(status)) {
            log.warn("二维码状态不正确，请先扫码, qrToken={}, status={}", qrToken, status);
            throw new BizException(ErrorCode.BAD_REQUEST, "请先扫码确认");
        }
        
        // 验证扫码用户与确认用户是否一致
        String scannedUserId = redisTemplate.opsForValue().get(USER_KEY_PREFIX + qrToken);
        if (scannedUserId != null && !scannedUserId.equals(String.valueOf(userId))) {
            log.warn("扫码用户与确认用户不一致, qrToken={}, scannedUserId={}, confirmUserId={}", 
                    qrToken, scannedUserId, userId);
            throw new BizException(ErrorCode.BAD_REQUEST, "二维码已被其他用户使用");
        }
        
        // 更新状态为已确认，不刷新TTL
        Long remainingTtl = redisTemplate.getExpire(ticketKey, java.util.concurrent.TimeUnit.SECONDS);
        if (remainingTtl != null && remainingTtl > 0) {
            redisTemplate.opsForValue().set(ticketKey, "CONFIRMED", java.time.Duration.ofSeconds(remainingTtl));
            redisTemplate.opsForValue().set(USER_KEY_PREFIX + qrToken, 
                    userId + ":" + tenantId + ":" + username, 
                    java.time.Duration.ofSeconds(remainingTtl));
        } else {
            throw new BizException(ErrorCode.BAD_REQUEST, "二维码已过期");
        }
        
        log.info("扫码登录确认成功, qrToken={}, userId={}, username={}, 剩余时间={}秒", 
                qrToken, userId, username, remainingTtl);
    }

    @Override
    public String getLoginStatus(String qrToken) {
        String ticketKey = TICKET_KEY_PREFIX + qrToken;
        String status = redisTemplate.opsForValue().get(ticketKey);
        return status == null ? "EXPIRED" : status;
    }

    @Override
    public String[] getUserInfo(String qrToken) {
        String key = USER_KEY_PREFIX + qrToken;
        String userInfo = redisTemplate.opsForValue().get(key);
        if (userInfo == null) {
            return null;
        }
        return userInfo.split(":");
    }

    @Override
    public void cleanup(String qrToken) {
        redisTemplate.delete(TICKET_KEY_PREFIX + qrToken);
        redisTemplate.delete(USER_KEY_PREFIX + qrToken);
        log.debug("清理扫码登录数据, qrToken={}", qrToken);
    }

    /**
     * 生成二维码图片（Base64编码）
     */
    private String generateQrCode(String content, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H);
            
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            
            byte[] imageBytes = outputStream.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (WriterException | IOException e) {
            log.error("二维码生成失败, content={}", content, e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "二维码生成失败");
        }
    }
}
