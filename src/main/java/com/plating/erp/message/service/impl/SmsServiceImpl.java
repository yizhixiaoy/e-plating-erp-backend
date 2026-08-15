package com.plating.erp.message.service.impl;

import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.message.entity.SmsRecordEntity;
import com.plating.erp.message.mapper.SmsRecordMapper;
import com.plating.erp.message.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SmsServiceImpl implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsServiceImpl.class);

    private final SmsRecordMapper smsRecordMapper;

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.sms.provider:aliyun}")
    private String smsProvider;

    @Value("${app.sms.aliyun.access-key-id:}")
    private String accessKeyId;

    @Value("${app.sms.aliyun.access-key-secret:}")
    private String accessKeySecret;

    @Value("${app.sms.aliyun.sign-name:}")
    private String signName;

    @Value("${app.sms.aliyun.template-code:}")
    private String templateCode;

    @Value("${app.sms.aliyun.endpoint:https://dysmsapi.aliyuncs.com}")
    private String endpoint;

    public SmsServiceImpl(SmsRecordMapper smsRecordMapper) {
        this.smsRecordMapper = smsRecordMapper;
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendSmsAsync(String receiverPhone, String smsType, String content,
                             Long tenantId, Long receiverUserId, Long operatorId) {
        if (!smsEnabled) {
            log.debug("短信功能未启用,跳过发送: phone={}", receiverPhone);
            return;
        }
        if (receiverPhone == null || receiverPhone.isBlank()) {
            log.warn("收件人手机号为空,跳过发送");
            return;
        }

        // 创建发送记录
        SmsRecordEntity record = new SmsRecordEntity();
        record.setTenantId(tenantId != null ? tenantId : 0L);
        record.setReceiverUserId(receiverUserId);
        record.setReceiverPhone(receiverPhone);
        record.setSmsType(smsType);
        record.setContent(content);
        record.setSendStatus(0);
        record.setRetryCount(0);
        record.setOperatorId(operatorId);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        smsRecordMapper.insert(record);

        try {
            // 调用短信服务商API发送
            doSendSms(receiverPhone, content);

            record.setSendStatus(1);
            record.setSentTime(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            smsRecordMapper.updateById(record);

            log.info("短信发送成功: phone={}, type={}", receiverPhone, smsType);
        } catch (Exception e) {
            record.setSendStatus(2);
            record.setFailReason(e.getMessage());
            record.setUpdatedAt(LocalDateTime.now());
            smsRecordMapper.updateById(record);

            log.error("短信发送失败: phone={}, type={}, error={}", receiverPhone, smsType, e.getMessage(), e);
        }
    }

    @Override
    public void sendVerificationCodeSms(String phone, String code, String scene, Long operatorId, Long tenantId) {
        if (!smsEnabled) {
            log.debug("短信功能未启用,跳过验证码发送: phone={}", phone);
            return;
        }

        // 使用实际场景作为短信类型（如 LOGIN/RESET_PASSWORD/BIND_PHONE）
        String smsType = scene != null ? scene : "VERIFICATION_CODE";
        String content = buildVerificationCodeContent(code);
        sendSmsAsync(phone, smsType, content, tenantId, operatorId, operatorId);
    }

    @Override
    public void sendResetPasswordSms(String phone, String realName, String newPassword,
                                     Long tenantId, Long receiverUserId, Long operatorId) {
        if (!smsEnabled) {
            log.debug("短信功能未启用,跳过重置密码短信: phone={}", phone);
            return;
        }

        String content = "{\"realName\":\"" + (realName != null ? realName : "")
                + "\",\"password\":\"" + newPassword + "\"}";
        sendSmsAsync(phone, "RESET_PASSWORD_NOTIFY", content, tenantId, receiverUserId, operatorId);
        log.info("重置密码短信已加入发送队列: phone={}", phone);
    }

    /**
     * 构建验证码短信内容
     * 当使用模板发送时，content字段作为模板参数记录
     */
    private String buildVerificationCodeContent(String code) {
        // 模板参数：{code} 为验证码，{minutes} 为有效期
        return "{\"code\":\"" + code + "\",\"minutes\":\"5\"}";
    }

    /**
     * 调用短信服务商API发送短信
     * 当前支持阿里云短信服务
     */
    private void doSendSms(String phone, String content) {
        if ("aliyun".equalsIgnoreCase(smsProvider)) {
            sendByAliyun(phone, content);
        } else {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "不支持的短信服务商: " + smsProvider);
        }
    }

    /**
     * 通过阿里云短信服务发送短信
     * 使用HTTP API调用，无需额外SDK依赖
     */
    private void sendByAliyun(String phone, String content) {
        try {
            // 构建公共参数
            Map<String, String> params = new TreeMap<>();
            params.put("AccessKeyId", accessKeyId);
            params.put("Timestamp", java.time.Instant.now().atZone(java.time.ZoneOffset.UTC)
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")));
            params.put("Format", "JSON");
            params.put("SignatureMethod", "HMAC-SHA1");
            params.put("SignatureVersion", "1.0");
            params.put("SignatureNonce", UUID.randomUUID().toString());
            params.put("Action", "SendSms");
            params.put("Version", "2017-05-25");

            // 业务参数
            params.put("PhoneNumbers", phone);
            params.put("SignName", signName);
            params.put("TemplateCode", templateCode);
            params.put("TemplateParam", content);

            // 计算签名
            String signature = computeSignature(params);
            params.put("Signature", signature);

            // 构建请求URL
            String queryString = params.entrySet().stream()
                    .map(e -> urlEncode(e.getKey()) + "=" + urlEncode(e.getValue()))
                    .collect(Collectors.joining("&"));

            URL url = new URL(endpoint + "/?" + queryString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("阿里云短信API调用失败, HTTP状态码: " + responseCode);
            }

            // 读取响应
            try (var is = conn.getInputStream()) {
                String responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                log.debug("阿里云短信API响应: {}", responseBody);
                // 简单校验响应中是否包含成功标识
                if (!responseBody.contains("\"Code\":\"OK\"")) {
                    throw new RuntimeException("短信发送失败, 响应: " + responseBody);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("调用阿里云短信服务异常: " + e.getMessage(), e);
        }
    }

    /**
     * 计算阿里云API签名 (HMAC-SHA1)
     */
    private String computeSignature(Map<String, String> params) {
        try {
            // 构造待签名字符串
            String sortedQueryString = params.entrySet().stream()
                    .map(e -> urlEncode(e.getKey()) + "=" + urlEncode(e.getValue()))
                    .collect(Collectors.joining("&"));

            String stringToSign = "GET&" + urlEncode("/") + "&" + urlEncode(sortedQueryString);

            // HMAC-SHA1签名
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec((accessKeySecret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] signBytes = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signBytes);
        } catch (Exception e) {
            throw new RuntimeException("计算短信API签名失败", e);
        }
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (Exception e) {
            return value;
        }
    }
}