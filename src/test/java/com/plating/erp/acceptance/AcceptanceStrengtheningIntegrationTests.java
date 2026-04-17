package com.plating.erp.acceptance;

import com.plating.erp.audit.AuditLogAspect;
import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.audit.entity.BizLogEntity;
import com.plating.erp.audit.entity.OperLogEntity;
import com.plating.erp.audit.mapper.BizLogMapper;
import com.plating.erp.audit.mapper.OperLogMapper;
import com.plating.erp.common.api.GlobalExceptionHandler;
import com.plating.erp.common.security.AuthzService;
import com.plating.erp.common.security.CredentialRevocationService;
import com.plating.erp.common.security.CurrentUser;
import com.plating.erp.common.security.JwtAuthenticationFilter;
import com.plating.erp.common.security.JwtTokenService;
import com.plating.erp.common.security.PermissionMapper;
import com.plating.erp.common.security.SecurityConfig;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.common.security.impl.AuthzServiceImpl;
import com.plating.erp.common.security.impl.CredentialRevocationServiceImpl;
import com.plating.erp.common.security.impl.JwtTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AcceptanceStrengtheningIntegrationTests.TestController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenServiceImpl.class,
        CredentialRevocationServiceImpl.class,
        AuthzServiceImpl.class,
        AuditLogAspect.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=ChangeThisJwtSecretAtLeast32Chars!",
        "app.jwt.expire-seconds=7200",
        "app.authz.cache-seconds=120"
})
class AcceptanceStrengtheningIntegrationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private PermissionMapper permissionMapper;
    @MockBean
    private OperLogMapper operLogMapper;
    @MockBean
    private BizLogMapper bizLogMapper;
    @MockBean
    private StringRedisTemplate redisTemplate;
    @MockBean
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
    }

    @Test
    void tenantIsolation_usesTenantFromToken() throws Exception {
        String token = jwtTokenService.createToken(10001L, 20001L, "a-admin", List.of("tenant_admin"));
        mockMvc.perform(get("/api/v1/test/tenant")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenTenantId", is(20001)))
                .andExpect(jsonPath("$.headerTenantId", is(99999)));
    }

    @Test
    void permissionDenied_whenPermMissing() throws Exception {
        when(permissionMapper.selectPerms(anyLong(), anyLong())).thenReturn(List.of());
        String token = jwtTokenService.createToken(10001L, 20001L, "a-admin", List.of("tenant_admin"));
        mockMvc.perform(get("/api/v1/test/perm")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditLog_persistsMaskedParams() throws Exception {
        when(permissionMapper.selectPerms(anyLong(), anyLong())).thenReturn(List.of("audit:write"));
        String token = jwtTokenService.createToken(10001L, 20001L, "a-admin", List.of("tenant_admin"));
        String body = """
                {
                  "username": "demo",
                  "password": "rawPass123",
                  "token": "abc-token",
                  "phone": "13800000000"
                }
                """;

        mockMvc.perform(post("/api/v1/test/audit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<OperLogEntity> captor = ArgumentCaptor.forClass(OperLogEntity.class);
        verify(operLogMapper, atLeastOnce()).insert(captor.capture());
        verify(bizLogMapper, atLeastOnce()).insert(org.mockito.ArgumentMatchers.any(BizLogEntity.class));

        String requestParams = captor.getValue().getRequestParams();
        assertTrue(requestParams.contains("\"password\":\"***\""));
        assertTrue(requestParams.contains("\"token\":\"***\""));
        assertTrue(requestParams.contains("\"phone\":\"***\""));
        assertFalse(requestParams.contains("rawPass123"));
        assertFalse(requestParams.contains("abc-token"));
        assertFalse(requestParams.contains("13800000000"));
    }

    @RestController
    @RequestMapping("/api/v1/test")
    static class TestController {
        @GetMapping("/tenant")
        @PreAuthorize("isAuthenticated()")
        public Map<String, Object> tenant(@RequestHeader(value = "X-Tenant-Id", required = false) Long headerTenantId) {
            CurrentUser user = SecurityUtils.currentUser();
            return Map.of("tokenTenantId", user.tenantId(), "headerTenantId", headerTenantId);
        }

        @GetMapping("/perm")
        @PreAuthorize("@authz.hasPerm('tenant:view')")
        public Map<String, Object> perm() {
            return Map.of("ok", true);
        }

        @PostMapping("/audit")
        @PreAuthorize("@authz.hasPerm('audit:write')")
        @AuditLog(module = "测试", operateType = "CREATE", bizModule = "test", fieldName = "payload")
        public Map<String, Object> audit(@RequestBody Map<String, Object> body) {
            return body;
        }
    }
}
