package com.plating.erp.common.security;

import com.plating.erp.common.tenant.TenantContextHolder;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
                Claims claims = jwtTokenService.parse(token);
                Long userId = Long.parseLong(claims.getSubject());
                Long tenantId = toLong(claims.get("tenantId"));
                Date issuedAt = claims.getIssuedAt();
                if (tenantId != null && issuedAt != null
                        && credentialRevocationService.isIssuedBeforeRevocation(tenantId, userId, issuedAt.getTime())) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                String username = claims.get("username", String.class);
                List<String> roles = claims.get("roles", List.class);
                Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (roles != null) {
                    roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                }
                CurrentUser currentUser = new CurrentUser(userId, tenantId, username, roles);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(currentUser, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                boolean isSystem = roles != null && roles.contains("system");
                if (!isSystem && tenantId != null) {
                    TenantContextHolder.setTenantId(tenantId);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
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
