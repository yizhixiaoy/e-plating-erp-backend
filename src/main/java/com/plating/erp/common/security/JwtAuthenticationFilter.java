package com.plating.erp.common.security;

import com.plating.erp.common.tenant.TenantContextHolder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtTokenService jwtTokenService;
    private final CredentialRevocationService credentialRevocationService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                     CredentialRevocationService credentialRevocationService) {
        this.jwtTokenService = jwtTokenService;
        this.credentialRevocationService = credentialRevocationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring(7);
                
                try {
                    // 尝试解析Token
                    Claims claims = jwtTokenService.parse(token);
                    processTokenClaims(claims, request, response);
                } catch (ExpiredJwtException e) {
                    // Token过期，返回401
                    log.debug("JWT token expired: {}", e.getMessage());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"Token已过期，请重新登录\"}");
                    return;
                } catch (Exception e) {
                    // Token无效或其他解析错误
                    log.warn("JWT token parse failed: {}", e.getMessage());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"Token无效\"}");
                    return;
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
    
    /**
     * 处理Token中的Claims信息
     */
    private void processTokenClaims(Claims claims, HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        Long userId = Long.parseLong(claims.getSubject());
        Long tenantId = toLong(claims.get("tenantId"));
        Integer userType = toInteger(claims.get("userType"));
        Date issuedAt = claims.getIssuedAt();
        
        // 检查凭证是否被撤销
        if (tenantId != null && issuedAt != null
                && credentialRevocationService.isIssuedBeforeRevocation(tenantId, userId, issuedAt.getTime())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"凭证已失效，请重新登录\"}");
            return;
        }
        
        String username = claims.get("username", String.class);
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (roles != null) {
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        }
        
        CurrentUser currentUser = new CurrentUser(userId, tenantId, username, roles, userType);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(currentUser, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 租户用户需要设置租户上下文，用于 MyBatis-Plus 租户拦截器
        // 平台用户（userType=0）跳过租户隔离，不设置 TenantContextHolder
        if (currentUser.isTenantUser() && tenantId != null) {
            TenantContextHolder.setTenantId(tenantId);
        }
    }

    private static Integer toInteger(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(raw.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long toLong(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(raw.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
