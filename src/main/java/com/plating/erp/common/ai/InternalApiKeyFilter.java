package com.plating.erp.common.ai;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 内部API Key校验过滤器
 * <p>
 * 拦截 /api/v1/ai/** 路径（Python AI服务回调Java查询业务数据），
 * 校验 X-Internal-Api-Key 请求头，防止未授权的内部调用。
 * <p>
 * 设计文档 5.3 节：双重认证 = 内部API Key + 用户JWT上下文
 */
@Slf4j
@Component
@Order(2)
public class InternalApiKeyFilter extends OncePerRequestFilter {

    @Value("${app.ai.internal-api-key:}")
    private String internalApiKey;

    /** 内部AI数据查询路径前缀 */
    private static final String AI_INTERNAL_PATH = "/api/v1/ai/";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws IOException {
        String path = request.getRequestURI();

        // 仅拦截内部AI数据查询路径
        if (!path.startsWith(AI_INTERNAL_PATH)) {
            try {
                chain.doFilter(request, response);
            } catch (ServletException e) {
                throw new IOException(e);
            }
            return;
        }

        // 校验内部API Key
        String apiKey = request.getHeader("X-Internal-Api-Key");
        if (internalApiKey == null || internalApiKey.isBlank()) {
            log.error("内部API Key未配置，请设置 app.ai.internal-api-key");
            response.setStatus(500);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":500,\"message\":\"内部API Key未配置\"}");
            return;
        }

        if (!internalApiKey.equals(apiKey)) {
            log.warn("内部API Key校验失败, 来源IP: {}", request.getRemoteAddr());
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"内部API Key无效\"}");
            return;
        }

        // 提取用户上下文（由Python服务从原始JWT中解析后透传）
        String tenantId = request.getHeader("X-Tenant-Id");
        String userId = request.getHeader("X-User-Id");
        String dataScope = request.getHeader("X-Data-Scope");

        if (tenantId != null) {
            request.setAttribute("ai.tenantId", Long.parseLong(tenantId));
        }
        if (userId != null) {
            request.setAttribute("ai.userId", Long.parseLong(userId));
        }
        if (dataScope != null) {
            request.setAttribute("ai.dataScope", dataScope);
        }

        log.debug("内部API调用验证通过, tenantId={}, userId={}, path={}", tenantId, userId, path);

        try {
            chain.doFilter(request, response);
        } catch (ServletException e) {
            throw new IOException(e);
        }
    }
}
