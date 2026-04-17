package com.plating.erp.common.security;

import io.jsonwebtoken.Claims;
import java.util.List;

public interface JwtTokenService {
    /**
     * 创建 JWT Token
     * 
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @param username 用户名
     * @param roles 角色列表
     * @param userType 用户类型（0=平台用户，1=租户用户）
     * @return JWT Token 字符串
     */
    String createToken(Long userId, Long tenantId, String username, List<String> roles, Integer userType);

    /**
     * 创建 JWT Token（兼容旧接口，userType 默认为租户用户）
     * @deprecated 使用新接口 {@link #createToken(Long, Long, String, List, Integer)}
     */
    @Deprecated
    default String createToken(Long userId, Long tenantId, String username, List<String> roles) {
        return createToken(userId, tenantId, username, roles, 1);
    }

    Claims parse(String token);

    Long getUserIdFromToken(String token);
}
