package com.plating.erp.common.security.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.security.CurrentUser;
import com.plating.erp.common.security.SessionKeyService;
import com.plating.erp.common.util.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.crypto.spec.SecretKeySpec;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 响应字段加密 Advice
 * 拦截所有 JSON 响应，将敏感字段加密后返回
 */
@RestControllerAdvice
public class CryptoResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final Logger log = LoggerFactory.getLogger(CryptoResponseAdvice.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /** 需要加密的响应字段名 */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "phone", "email", "realName", "username",
            "companyPhone", "leaderName", "newPassword"
    );

    private final SessionKeyService sessionKeyService;

    public CryptoResponseAdvice(SessionKeyService sessionKeyService) {
        this.sessionKeyService = sessionKeyService;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true; // 处理所有 JSON 响应
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) return null;

        // 跳过登录类接口：sessionKey 和 userInfo 在同一个响应中，前端无法解密
        String path = request.getURI().getPath();
        if (path != null && (path.contains("/auth/login") || path.contains("/auth/scan-status"))) {
            return body;
        }

        SecretKeySpec key = getSessionKey();
        if (key == null) return body;

        // 每次加密响应时确保 session key 持久化（移除旧版本可能设置的 TTL）
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CurrentUser user) {
                sessionKeyService.touch(user.userId(), user.tenantId());
            }
        } catch (Exception ignored) {
        }

        try {
            // 只处理 ApiResponse 的 data 字段
            if (body instanceof ApiResponse<?> apiResp && apiResp.data() != null) {
                JsonNode dataNode = MAPPER.valueToTree(apiResp.data());
                JsonNode encrypted = encryptFields(dataNode, key);
                Object encryptedData = MAPPER.treeToValue(encrypted, Object.class);
                return new ApiResponse<>(apiResp.code(), apiResp.msg(), encryptedData,
                        apiResp.traceId(), apiResp.timestamp());
            }
            return body;
        } catch (Exception e) {
            log.warn("[crypto] response encrypt failed, pass through", e);
            return body;
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
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** 递归加密 JSON 节点中的敏感字段 */
    private JsonNode encryptFields(JsonNode node, SecretKeySpec key) {
        if (node.isObject()) {
            ObjectNode obj = MAPPER.createObjectNode();
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = it.next();
                String fieldName = entry.getKey();
                JsonNode value = entry.getValue();
                if (SENSITIVE_FIELDS.contains(fieldName) && value.isTextual() && !value.asText().isEmpty()) {
                    try {
                        obj.put(fieldName, CryptoUtil.encrypt(key, value.asText()));
                    } catch (Exception e) {
                        obj.set(fieldName, value); // 加密失败用原文
                    }
                } else if (value.isObject() || value.isArray()) {
                    obj.set(fieldName, encryptFields(value, key));
                } else {
                    obj.set(fieldName, value);
                }
            }
            return obj;
        }
        return node;
    }
}
