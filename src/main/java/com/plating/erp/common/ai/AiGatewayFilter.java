package com.plating.erp.common.ai;

import com.plating.erp.common.security.JwtTokenService;
import com.plating.erp.iam.entity.MenuEntity;
import com.plating.erp.iam.mapper.MenuMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.*;

/**
 * HTTP hop-by-hop 头（逐跳头），代理转发时必须移除，
 * 这些头仅适用于单次连接，不可被中间代理透传。
 */
class HopByHopHeaders {
    static final Set<String> HEADERS = Set.of(
        "transfer-encoding", "connection", "keep-alive",
        "proxy-authenticate", "proxy-authorization",
        "te", "trailer", "upgrade"
    );
}

/**
 * AI网关过滤器 - 拦截 /api/ai/** 请求，校验JWT并代理转发至Python AI服务
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AiGatewayFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final MenuMapper menuMapper;

    @Value("${app.ai.service-url:http://127.0.0.1:8000}")
    private String aiServiceUrl;

    /** 允许未登录访问的AI路径（仅基础问答） */
    private static final Set<String> GUEST_ALLOWED_PATHS = Set.of("/api/ai/chat");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws IOException {
        String path = request.getRequestURI();

        // 仅拦截 /api/ai/** 路径
        if (!path.startsWith("/api/ai/")) {
            try { chain.doFilter(request, response); } catch (ServletException e) { throw new IOException(e); }
            return;
        }

        String token = extractToken(request);

        if (token == null || token.isBlank()) {
            // 未登录：仅允许基础问答路径
            if (!GUEST_ALLOWED_PATHS.contains(path)) {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"请先登录后使用完整AI功能\"}");
                return;
            }
            forwardAsGuest(request, response);
            return;
        }

        // JWT校验
        Claims claims;
        try {
            claims = jwtTokenService.parse(token);
        } catch (ExpiredJwtException e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token已过期，请重新登录\"}");
            return;
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token无效\"}");
            return;
        }

        // 转发至Python AI服务
        forwardToAiService(request, response, claims);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void forwardAsGuest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Mode", "guest");
        headers.set("X-Tenant-Id", "0");
        headers.set("X-User-Id", "0");
        headers.set("X-Username", "guest");
        headers.set("X-Nickname", "访客");
        headers.set("X-Roles", "");
        headers.set("X-Permissions", "");
        headers.set("X-Data-Scope", "NONE");
        log.info("AI网关转发 - 访客模式访问");
        forwardToPython(request, response, headers);
    }

    @SuppressWarnings("unchecked")
    private void forwardToAiService(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Claims claims) throws IOException {
        Long userId = Long.parseLong(claims.getSubject());
        Long tenantId = claims.get("tenantId") != null ? Long.valueOf(claims.get("tenantId").toString()) : 0L;
        String username = claims.get("username") != null ? claims.get("username").toString() : "";
        List<String> roles = claims.get("roles") != null ? (List<String>) claims.get("roles") : Collections.emptyList();
        Integer userType = claims.get("userType") != null ? Integer.valueOf(claims.get("userType").toString()) : 1;

        // 与MenuServiceImpl保持一致的权限查询逻辑
        List<String> permissions;
        try {
            if (userType == 0) {
                // 平台管理员(userType=0)：获取所有菜单权限（平台级+租户级）
                log.info("AI网关转发 - 平台管理员获取全部权限: userId={}", userId);
                List<MenuEntity> allMenus = menuMapper.selectAllMenus();
                permissions = allMenus.stream()
                    .filter(m -> "F".equals(m.getMenuType()) && m.getPerms() != null)
                    .map(MenuEntity::getPerms)
                    .distinct()
                    .toList();
            } else {
                // 租户用户：根据角色关联获取权限
                log.info("AI网关转发 - 租户用户获取角色权限: userId={}, tenantId={}", userId, tenantId);
                permissions = menuMapper.selectPermsByUserId(userId, tenantId);
            }
            log.info("AI网关转发 - 权限查询完成: userId={}, userType={}, 权限数={}", 
                     userId, userType, permissions.size());
        } catch (Exception e) {
            log.error("AI网关转发 - 查询权限失败，使用JWT中的权限: userId={}, tenantId={}", userId, tenantId, e);
            // 降级：使用JWT中的权限（向后兼容）
            permissions = claims.get("permissions") != null ? (List<String>) claims.get("permissions") : Collections.emptyList();
        }

        // 调试日志：记录权限信息
        if (log.isDebugEnabled()) {
            log.debug("AI网关转发 - 用户: {}, 角色: {}, 权限列表: {}", username, roles, permissions);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Mode", "authenticated");
        headers.set("X-Tenant-Id", String.valueOf(tenantId));
        headers.set("X-User-Id", String.valueOf(userId));
        headers.set("X-Username", username);
        headers.set("X-Roles", String.join(",", roles));
        headers.set("X-Permissions", String.join(",", permissions));
        headers.set("Authorization", request.getHeader("Authorization"));

        forwardToPython(request, response, headers);
    }

    private void forwardToPython(HttpServletRequest request,
                                  HttpServletResponse response,
                                  HttpHeaders headers) throws IOException {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        URI targetUri = URI.create(aiServiceUrl + path + (query != null ? "?" + query : ""));

        // 读取请求体（必须在连接建立前读取，避免流被消费）
        byte[] body = request.getInputStream().readAllBytes();

        // 重试机制：Connection reset 时自动重试一次
        int maxRetries = 2;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) targetUri.toURL().openConnection();
                conn.setRequestMethod(request.getMethod());
                conn.setDoInput(true);
                // 禁用 keep-alive：避免复用已被对端关闭的连接导致 Connection reset
                conn.setRequestProperty("Connection", "close");
                conn.setConnectTimeout(30_000);
                conn.setReadTimeout(300_000); // SSE 长连接读超时 5 分钟

                // 拷贝请求头（含认证上下文）
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    for (String val : entry.getValue()) {
                        conn.setRequestProperty(entry.getKey(), val);
                    }
                }
                String contentType = request.getContentType();
                if (contentType != null) {
                    conn.setRequestProperty("Content-Type", contentType);
                }

                // 写入请求体
                if (body.length > 0) {
                    conn.setDoOutput(true);
                    conn.setFixedLengthStreamingMode(body.length);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(body);
                        os.flush();
                    }
                }

                // 读取响应状态码
                int statusCode = conn.getResponseCode();
                response.setStatus(statusCode);

                // 拷贝非 hop-by-hop 响应头（大小写不敏感匹配，uvicorn 返回的是小写头名）
                conn.getHeaderFields().forEach((k, v) -> {
                    if (k == null || HopByHopHeaders.HEADERS.contains(k.toLowerCase())) return;
                    v.forEach(val -> response.addHeader(k, val));
                });

                // 流式转发响应体（SSE 场景：chunk 实时透传）
                try (InputStream is = (statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream());
                     OutputStream os = response.getOutputStream()) {
                    if (is != null) {
                        byte[] buf = new byte[8192];
                        int n;
                        try {
                            while ((n = is.read(buf)) != -1) {
                                os.write(buf, 0, n);
                                os.flush(); // 关键：每次读完 chunk 立即 flush，确保 SSE 事件实时到达前端
                            }
                        } catch (java.io.EOFException eof) {
                            // SSE 流正常结束：uvicorn 关闭连接时 HttpURLConnection 可能抛 Premature EOF，
                            // 此时数据已全部透传给前端，视为正常完成
                            log.debug("AI代理流式传输结束(EOF): {}", eof.getMessage());
                        }
                    }
                }
                return; // 成功，直接返回
            } catch (java.net.SocketException e) {
                // 响应已提交（已开始流式输出）：客户端中途断开，重试无意义，直接结束
                if (response.isCommitted()) {
                    log.warn("AI代理流式输出中断，客户端已断开({})", e.getMessage());
                    if (conn != null) conn.disconnect();
                    return;
                }
                // 连接建立阶段失败 → 重试
                lastException = e;
                log.warn("AI服务代理第{}次尝试失败({})，{}", attempt, e.getMessage(),
                    attempt < maxRetries ? "将重试" : "已耗尽重试");
                if (conn != null) conn.disconnect();
                if (attempt < maxRetries) {
                    try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            } catch (java.io.EOFException eof) {
                // 如果在 getResponseCode() 阶段就 EOF，说明连接已断 → 重试
                if (response.isCommitted()) {
                    log.debug("AI代理响应已完成(EOF)");
                    return;
                }
                lastException = eof;
                log.warn("AI服务代理第{}次尝试Premature EOF，{}", attempt,
                    attempt < maxRetries ? "将重试" : "已耗尽重试");
                if (conn != null) conn.disconnect();
                if (attempt < maxRetries) {
                    try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            } catch (Exception e) {
                lastException = e;
                log.error("AI服务代理转发失败: {}", e.getMessage());
                if (conn != null) conn.disconnect();
                break; // 非网络异常，不重试
            }
        }

        // 所有重试均失败
        log.error("AI服务代理转发最终失败(重试{}次): {}", maxRetries, 
            lastException != null ? lastException.getMessage() : "unknown");
        if (!response.isCommitted()) {
            response.reset();
            response.setStatus(502);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":502,\"message\":\"AI服务暂时不可用\"}");
        }
    }
}
