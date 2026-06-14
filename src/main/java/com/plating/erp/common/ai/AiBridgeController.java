package com.plating.erp.common.ai;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI数据查询桥接控制器
 * <p>
 * 接收Python AI服务的内部数据查询请求（经 InternalApiKeyFilter 校验后到达），
 * 提供只读的业务数据查询能力。
 * <p>
 * 端点：POST /api/v1/ai/data-query
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiBridgeController {

    private final AiBridgeService aiBridgeService;

    /**
     * 业务数据查询
     * <p>
     * 请求体示例：
     * <pre>
     * {
     *   "table": "sys_user",
     *   "aggregate": {"func": "count", "field": "*"},
     *   "filters": {"date_range": ["2026-01-01", "2026-05-31"], "status": 1}
     * }
     * </pre>
     *
     * @param body    查询参数
     * @param request HTTP请求（用于获取InternalApiKeyFilter注入的上下文属性）
     * @return 查询结果
     */
    @PostMapping("/data-query")
    public Map<String, Object> dataQuery(@RequestBody Map<String, Object> body,
                                          HttpServletRequest request) {
        // 从请求属性中提取用户上下文（由InternalApiKeyFilter注入）
        Long tenantId = (Long) request.getAttribute("ai.tenantId");
        Long userId = (Long) request.getAttribute("ai.userId");
        String dataScope = (String) request.getAttribute("ai.dataScope");

        log.info("AI数据查询请求: table={}, tenantId={}, userId={}",
                body.get("table"), tenantId, userId);

        // 提取查询参数
        String table = String.valueOf(body.getOrDefault("table", ""));
        @SuppressWarnings("unchecked")
        Map<String, Object> aggregate = (Map<String, Object>) body.get("aggregate");
        @SuppressWarnings("unchecked")
        Map<String, Object> filters = (Map<String, Object>) body.get("filters");

        try {
            // 调用服务层执行查询
            Map<String, Object> result = aiBridgeService.queryData(
                    table, aggregate, filters, tenantId, dataScope);

            // 包装响应（Python端读取 response.json()["result"]）
            return Map.of(
                    "code", 200,
                    "result", result
            );
        } catch (IllegalArgumentException e) {
            log.warn("AI数据查询参数校验失败: {}", e.getMessage());
            return Map.of(
                    "code", 400,
                    "message", e.getMessage()
            );
        }
    }

    /**
     * 获取可查询的数据Schema
     * <p>
     * 返回白名单中所有表名、中文标签和字段列表。
     * Python AI服务在启动时调用此接口缓存Schema，
     * 并在工具选择时注入到LLM prompt中。
     */
    @GetMapping("/schema")
    public Map<String, Object> getSchema() {
        log.debug("AI Schema查询请求");
        return aiBridgeService.getAvailableSchema();
    }
}
