package com.plating.erp.common.security;

/**
 * 通信会话密钥管理
 * 用于前后端敏感字段 SM4-CBC 加解密的密钥存储
 */
public interface SessionKeyService {

    /** 生成并存储会话密钥，返回 hex 编码的 key */
    String create(Long userId, Long tenantId);

    /** 获取会话密钥（hex），不存在返回 null */
    String get(Long userId, Long tenantId);

    /** 刷新会话密钥的过期时间（使用配置的 TTL 重新计算） */
    void touch(Long userId, Long tenantId);

    /** 移除会话密钥 */
    void remove(Long userId, Long tenantId);
}
