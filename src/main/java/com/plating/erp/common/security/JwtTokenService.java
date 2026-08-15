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
     * @param permissions 权限标识列表
     * @param userType 用户类型（0=平台用户，1=租户用户）
     * @return JWT Token 字符串
     */
    String createToken(Long userId, Long tenantId, String username, List<String> roles, List<String> permissions, Integer userType);

    /**
     * 创建 JWT Token（permissions 默认为空列表）
     */
    default String createToken(Long userId, Long tenantId, String username, List<String> roles, Integer userType) {
        return createToken(userId, tenantId, username, roles, List.of(), userType);
    }

    Claims parse(String token);

    Long getUserIdFromToken(String token);
}
