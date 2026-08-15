package com.plating.erp.common.security.impl;

import com.plating.erp.common.security.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Date;
import java.util.List;

@Service
public class JwtTokenServiceImpl implements JwtTokenService {
    private final SecretKey key;
    private final long expireSeconds;

    public JwtTokenServiceImpl(@Value("${app.jwt.secret}") String secret,
                               @Value("${app.jwt.expire-seconds}") long expireSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = expireSeconds;
    }

    @Override
    public String createToken(Long userId, Long tenantId, String username, List<String> roles, List<String> permissions, Integer userType) {
        long now = System.currentTimeMillis();
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("tenantId", tenantId);
        claims.put("username", username);
        claims.put("roles", roles);
        claims.put("permissions", permissions != null ? permissions : List.of());
        claims.put("userType", userType);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(claims)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireSeconds * 1000))
                .signWith(key)
                .compact();
    }

    @Override
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    @Override
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parse(token);
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }
}
