package com.plating.erp.common.security.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.plating.erp.common.security.CurrentUser;
import com.plating.erp.common.security.SessionKeyService;
import com.plating.erp.common.util.CryptoUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 请求字段解密 Filter
 * 在 JwtAuthenticationFilter 之后执行，读取加密的请求体字段并解密
 */
public class CryptoRequestFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(CryptoRequestFilter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 需要解密的请求字段名 */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "oldPassword", "newPassword",
            "phone", "email", "smsCode", "emailCode",
            "realName", "username"
    );

    private final SessionKeyService sessionKeyService;

    public CryptoRequestFilter(SessionKeyService sessionKeyService) {
        this.sessionKeyService = sessionKeyService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        String path = httpReq.getRequestURI();

        // 跳过非 JSON 请求
        String contentType = httpReq.getContentType();
        if (contentType == null || !contentType.contains("application/json")) {
            chain.doFilter(request, response);
            return;
        }

        // 获取 sessionKey
        SecretKeySpec key = getSessionKey();
        if (key == null) {
            log.debug("[crypto] sessionKey not found, skip decrypt, path={}", path);
            chain.doFilter(request, response);
            return;
        }

        // 读取 body
        String body = httpReq.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        if (body.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        try {
            JsonNode root = MAPPER.readTree(body);
            JsonNode decrypted = decryptFields(root, key);
            String newBody = MAPPER.writeValueAsString(decrypted);
            log.debug("[crypto] decrypt success, path={}", path);
            chain.doFilter(new CachedBodyRequest(httpReq, newBody), response);
        } catch (Exception e) {
            log.warn("[crypto] request decrypt failed, path={}", path, e);
            chain.doFilter(new CachedBodyRequest(httpReq, body), response);
        }
    }

    private SecretKeySpec getSessionKey() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CurrentUser user) {
                String raw = sessionKeyService.get(user.userId(), user.tenantId());
                if (raw != null) {
                    return CryptoUtil.parseKey(raw);
                }
                log.debug("[crypto] sessionKey not found in redis, userId={}, tenantId={}", user.userId(), user.tenantId());
            } else {
                log.debug("[crypto] no authenticated user in SecurityContext");
            }
        } catch (Exception e) {
            log.debug("[crypto] getSessionKey error", e);
        }
        return null;
    }

    /** 递归解密 JSON 节点中的敏感字段 */
    private JsonNode decryptFields(JsonNode node, SecretKeySpec key) {
        if (node.isObject()) {
            ObjectNode obj = MAPPER.createObjectNode();
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = it.next();
                String fieldName = entry.getKey();
                JsonNode value = entry.getValue();
                if (SENSITIVE_FIELDS.contains(fieldName) && value.isTextual()) {
                    try {
                        obj.put(fieldName, CryptoUtil.decrypt(key, value.asText()));
                    } catch (Exception e) {
                        obj.set(fieldName, value); // 解密失败用原文
                    }
                } else if (value.isObject() || value.isArray()) {
                    obj.set(fieldName, decryptFields(value, key));
                } else {
                    obj.set(fieldName, value);
                }
            }
            return obj;
        } else if (node.isArray()) {
            return node; // 数组不处理，敏感字段不会出现在数组中
        }
        return node;
    }

    /** 可缓存的请求体包装器 */
    private static class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request, String body) {
            super(request);
            this.body = body.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bis = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return bis.read(); }
                @Override public boolean isFinished() { return bis.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {}
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), StandardCharsets.UTF_8));
        }
    }
}
