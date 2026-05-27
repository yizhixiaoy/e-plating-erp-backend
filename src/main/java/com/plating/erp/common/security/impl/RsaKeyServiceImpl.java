package com.plating.erp.common.security.impl;

import com.plating.erp.common.security.RsaKeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;

/**
 * RSA 密钥对管理实现
 * 密钥对存储在 Redis 中，短 TTL，一次性使用
 */
@Service
public class RsaKeyServiceImpl implements RsaKeyService {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyServiceImpl.class);
    private static final String PRIVATE_KEY_PREFIX = "erp:rsa:priv:";
    private static final String PUBLIC_KEY_PREFIX = "erp:rsa:pub:";
    private static final Duration KEY_TTL = Duration.ofMinutes(3);
    private static final int RSA_KEY_SIZE = 2048;

    private final StringRedisTemplate redisTemplate;

    public RsaKeyServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String generateKeyPair(String clientId) {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(RSA_KEY_SIZE, new SecureRandom());
            KeyPair pair = gen.generateKeyPair();

            String pubBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
            String privBase64 = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());

            redisTemplate.opsForValue().set(PRIVATE_KEY_PREFIX + clientId, privBase64, KEY_TTL);
            redisTemplate.opsForValue().set(PUBLIC_KEY_PREFIX + clientId, pubBase64, KEY_TTL);

            log.debug("RSA 密钥对已生成, clientId={}", clientId);
            return pubBase64;
        } catch (Exception e) {
            throw new RuntimeException("RSA 密钥对生成失败", e);
        }
    }

    @Override
    public String getPrivateKey(String clientId) {
        return redisTemplate.opsForValue().get(PRIVATE_KEY_PREFIX + clientId);
    }

    @Override
    public String decrypt(String clientId, String encryptedBase64) {
        try {
            String privBase64 = getPrivateKey(clientId);
            if (privBase64 == null) {
                log.warn("RSA 私钥不存在或已过期, clientId={}", clientId);
                return null;
            }

            // 一次性使用：解密后立即销毁密钥对
            redisTemplate.delete(PRIVATE_KEY_PREFIX + clientId);
            redisTemplate.delete(PUBLIC_KEY_PREFIX + clientId);

            byte[] privBytes = Base64.getDecoder().decode(privBase64);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] plainBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedBase64));

            return new String(plainBytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("RSA 解密失败, clientId={}", clientId, e);
            return null;
        }
    }
}
