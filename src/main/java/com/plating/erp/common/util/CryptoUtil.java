package com.plating.erp.common.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;

/**
 * 国密 SM4-CBC 加解密工具
 *
 * 加密格式：hex( IV(16B) + ciphertext )
 * IV 随机生成，每次加密不同，与前端 sm-crypto 互通
 *
 * SM4 密钥长度：128 bits（32 hex chars）
 */
public final class CryptoUtil {

    private static final String ALGORITHM = "SM4";
    private static final String TRANSFORMATION = "SM4/CBC/PKCS7Padding";
    private static final int SM4_KEY_BYTES = 16;  // 128 bits
    private static final int SM4_IV_BYTES = 16;   // 128 bits

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private CryptoUtil() {
    }

    /** 生成一个 SM4 会话密钥，返回 hex 字符串（32 chars） */
    public static String generateSessionKey() {
        byte[] keyBytes = new byte[SM4_KEY_BYTES];
        new SecureRandom().nextBytes(keyBytes);
        return bytesToHex(keyBytes);
    }

    /** 将 hex 字符串还原为 SM4 SecretKey */
    public static SecretKeySpec parseKey(String hexKey) {
        if (hexKey == null || hexKey.length() != SM4_KEY_BYTES * 2) {
            throw new IllegalArgumentException("Invalid SM4 key length, expected 32 hex chars");
        }
        return new SecretKeySpec(hexToBytes(hexKey), ALGORITHM);
    }

    /** SM4-CBC 加密，返回 hex( IV(32 chars) + ciphertext ) */
    public static String encrypt(SecretKeySpec key, String plaintext) {
        try {
            byte[] iv = new byte[SM4_IV_BYTES];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION, BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(iv) + bytesToHex(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("SM4 encrypt failed", e);
        }
    }

    /** SM4-CBC 解密，输入 hex( IV + ciphertext ) */
    public static String decrypt(SecretKeySpec key, String encryptedHex) {
        try {
            if (encryptedHex == null || encryptedHex.length() < SM4_IV_BYTES * 2 + 1) {
                throw new IllegalArgumentException("Invalid ciphertext length");
            }
            String ivHex = encryptedHex.substring(0, SM4_IV_BYTES * 2);
            String cipherHex = encryptedHex.substring(SM4_IV_BYTES * 2);

            IvParameterSpec ivSpec = new IvParameterSpec(hexToBytes(ivHex));
            Cipher cipher = Cipher.getInstance(TRANSFORMATION, BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
            byte[] plain = cipher.doFinal(hexToBytes(cipherHex));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("SM4 decrypt failed", e);
        }
    }

    // ---------- hex 转换 ----------

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
