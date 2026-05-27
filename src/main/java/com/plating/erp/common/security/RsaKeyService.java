package com.plating.erp.common.security;

/**
 * RSA 密钥对管理
 * 用于登录时前端加密密码传输，私钥不离开后端
 */
public interface RsaKeyService {

    /**
     * 生成 RSA 密钥对并存储到 Redis，返回公钥（Base64 编码）
     * @param clientId 客户端标识（用于关联密钥对）
     * @return Base64 编码的公钥
     */
    String generateKeyPair(String clientId);

    /**
     * 根据 clientId 获取私钥（Base64 编码），不存在返回 null
     */
    String getPrivateKey(String clientId);

    /**
     * 使用私钥解密 RSA 密文，解密后自动销毁密钥对（一次性使用）
     * @param clientId 客户端标识
     * @param encryptedBase64 Base64 编码的密文
     * @return 明文，解密失败返回 null
     */
    String decrypt(String clientId, String encryptedBase64);
}
