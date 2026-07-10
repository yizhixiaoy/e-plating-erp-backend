package com.plating.erp.common.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI数据查询桥接服务
 * <p>
 * 为Python AI助理提供只读的业务数据查询能力。
 * 实施设计文档 5.2-5.4 节的安全控制：表名白名单、字段白名单、行数上限、租户隔离。
 * <p>
 * 白名单配置从字典表读取（dict_type: ai_table_whitelist / ai_field_whitelist），
 * 首次查询时加载到内存缓存，每5分钟自动刷新。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiBridgeService {

    private final JdbcTemplate jdbcTemplate;

    /** 单次查询最大返回行数 */
    private static final int MAX_LIMIT = 1000;

    /** 缓存有效期（毫秒）：5分钟 */
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    /**
     * 无逻辑删除列的表（审计日志类表不做逻辑删除）
     * 这些表在查询时不追加 deleted=0 条件
     */
    private static final Set<String> NO_DELETED_COLUMN = Set.of("sys_biz_log", "sys_oper_log");

    /**
     * 无tenant_id列的表（全局表不参与租户隔离）
     * 这些表在查询时不追加 tenant_id=? 条件
     */
    private static final Set<String> NO_TENANT_COLUMN = Set.of("sys_tenant");

    /**
     * 货物开单相关表不在AI桥接白名单中（通过字典配置动态管理）
     * 新的货物相关表白名单已在 Flyway V3 迁移脚本的 base_dict_item 中配置
     */

    /**
     * 聚合函数白名单
     */
    private static final Set<String> AGG_FUNC_WHITELIST = Set.of("count", "sum", "avg", "max", "min");

    // ========== 缓存字段 ==========
    private volatile Set<String> cachedTables = Set.of();
    private volatile Map<String, Set<String>> cachedFields = Map.of();
    private volatile Map<String, String> cachedTableLabels = Map.of(); // 表名→中文标签
    private volatile long cacheLoadedAt = 0;

    /**
     * 刷新白名单缓存（供外部调用，如字典数据变更后手动刷新）
     */
    public void refreshCache() {
        cacheLoadedAt = 0;
        ensureCacheLoaded();
    }

    /**
     * 执行数据查询
     *
     * @param table      表名（需在白名单内）
     * @param aggregate  聚合参数 {func, field}
     * @param filters    筛选条件 {date_range, status, ...}
     * @param tenantId   租户ID（数据隔离）
     * @param dataScope  数据范围
     * @return 查询结果
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> queryData(String table,
                                          Map<String, Object> aggregate,
                                          Map<String, Object> filters,
                                          Long tenantId,
                                          String dataScope) {
        ensureCacheLoaded();

        // 1. 校验表名白名单
        if (table == null || !cachedTables.contains(table)) {
            log.warn("AI查询被拒：表不在白名单中, table={}", table);
            throw new IllegalArgumentException("表 " + table + " 不在可查询白名单中");
        }

        // 2. 获取该表的字段白名单
        Set<String> allowedFields = cachedFields.getOrDefault(table, Set.of("id", "created_at"));

        // 3. 处理聚合
        String aggField = null;
        String aggFunc = null;
        if (aggregate != null && !aggregate.isEmpty()) {
            aggFunc = String.valueOf(aggregate.getOrDefault("func", "count")).toLowerCase();
            aggField = String.valueOf(aggregate.getOrDefault("field", "*"));

            if (!AGG_FUNC_WHITELIST.contains(aggFunc)) {
                throw new IllegalArgumentException("不支持的聚合函数: " + aggFunc);
            }
            if (!"*".equals(aggField) && !allowedFields.contains(aggField)) {
                throw new IllegalArgumentException("字段 " + aggField + " 不可查询");
            }
        }

        // 4. 构建SQL
        StringBuilder sqlBuilder = new StringBuilder();
        List<Object> params = new ArrayList<>();

        if (aggFunc != null && aggField != null) {
            String selectField = "*".equals(aggField) ? "*" : "`" + aggField + "`";
            sqlBuilder.append("SELECT ").append(aggFunc.toUpperCase())
                    .append("(").append(selectField).append(") AS agg_result FROM `")
                    .append(table).append("` WHERE 1=1");
        } else {
            String fieldList = String.join(", ", allowedFields.stream()
                    .map(f -> "`" + f + "`").toList());
            sqlBuilder.append("SELECT ").append(fieldList)
                    .append(" FROM `").append(table)
                    .append("` WHERE 1=1");
        }

        // 5. 逻辑删除过滤（仅对有deleted列的表）
        if (!NO_DELETED_COLUMN.contains(table)) {
            sqlBuilder.append(" AND deleted = 0");
        }

        // 6. 租户数据隔离（仅对有tenant_id列的表）
        if (!NO_TENANT_COLUMN.contains(table) && tenantId != null && tenantId > 0) {
            sqlBuilder.append(" AND tenant_id = ?");
            params.add(tenantId);
        }

        // 7. 应用筛选条件
        if (filters != null && !filters.isEmpty()) {
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if ("date_range".equals(key) && value instanceof List<?> dates && dates.size() == 2) {
                    sqlBuilder.append(" AND created_at >= ? AND created_at <= ?");
                    params.add(dates.get(0));
                    params.add(dates.get(1));
                } else if (allowedFields.contains(key)) {
                    sqlBuilder.append(" AND `").append(key).append("` = ?");
                    params.add(value);
                }
            }
        }

        // 8. 限制行数
        sqlBuilder.append(" LIMIT ").append(MAX_LIMIT);

        String sql = sqlBuilder.toString();
        log.debug("AI数据查询SQL: {} | params={}", sql, params);

        // 9. 执行查询
        if (aggFunc != null) {
            Object result = jdbcTemplate.queryForObject(sql, Object.class, params.toArray());
            return Map.of("aggregate", Map.of(
                    "func", aggFunc,
                    "field", aggField != null ? aggField : "*",
                    "value", result != null ? result : 0
            ));
        } else {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());
            return Map.of(
                    "rows", rows,
                    "total", rows.size()
            );
        }
    }

    // ========== 缓存加载 ==========

    private void ensureCacheLoaded() {
        long now = System.currentTimeMillis();
        if (now - cacheLoadedAt > CACHE_TTL_MS || cachedTables.isEmpty()) {
            synchronized (this) {
                if (now - cacheLoadedAt > CACHE_TTL_MS || cachedTables.isEmpty()) {
                    loadWhitelistFromDict();
                    cacheLoadedAt = now;
                }
            }
        }
    }

    /**
     * 从字典表加载白名单配置
     * <p>
     * 使用JdbcTemplate直接查询，绕过MyBatis-Plus租户拦截器，
     * 查询系统字典（tenant_id=0, status=0, deleted=0）
     */
    private void loadWhitelistFromDict() {
        try {
            // 加载表名白名单: dict_type='ai_table_whitelist'，同时读取label作为表中文名
            List<Map<String, Object>> tableRows = jdbcTemplate.queryForList(
                    "SELECT dict_value, dict_label FROM base_dict_item WHERE dict_type = ? AND status = 0 AND deleted = 0 AND tenant_id = 0",
                    "ai_table_whitelist"
            );
            Set<String> tables = new LinkedHashSet<>();
            Map<String, String> labels = new HashMap<>();
            for (Map<String, Object> row : tableRows) {
                String value = (String) row.get("dict_value");
                String label = (String) row.getOrDefault("dict_label", value);
                tables.add(value);
                labels.put(value, label);
            }
            cachedTables = tables;
            cachedTableLabels = labels;

            // 加载字段白名单: dict_type='ai_field_whitelist', dict_value格式为'表名.字段名'
            List<String> fields = jdbcTemplate.queryForList(
                    "SELECT dict_value FROM base_dict_item WHERE dict_type = ? AND status = 0 AND deleted = 0 AND tenant_id = 0",
                    String.class, "ai_field_whitelist"
            );
            Map<String, Set<String>> fieldMap = new HashMap<>();
            for (String fieldValue : fields) {
                int dotIdx = fieldValue.indexOf('.');
                if (dotIdx > 0 && dotIdx < fieldValue.length() - 1) {
                    String tableName = fieldValue.substring(0, dotIdx);
                    String fieldName = fieldValue.substring(dotIdx + 1);
                    fieldMap.computeIfAbsent(tableName, k -> new LinkedHashSet<>()).add(fieldName);
                }
            }
            cachedFields = new ConcurrentHashMap<>(fieldMap);

            log.info("AI白名单缓存加载完成: {}张表, {}个字段配置",
                    cachedTables.size(), fields.size());
        } catch (Exception e) {
            log.error("从字典加载AI白名单失败，使用空缓存", e);
            cachedTables = Set.of();
            cachedFields = Map.of();
            cachedTableLabels = Map.of();
        }
    }

    /**
     * 获取可用数据Schema（表名+中文标签+字段列表）
     * <p>
     * 供Python AI服务在启动时或工具选择时调用，
     * 让LLM了解可查询的表结构和字段。
     *
     * @return Schema信息
     */
    public Map<String, Object> getAvailableSchema() {
        ensureCacheLoaded();
        List<Map<String, Object>> tableList = new ArrayList<>();
        for (String table : cachedTables) {
            Map<String, Object> tableInfo = new LinkedHashMap<>();
            tableInfo.put("name", table);
            tableInfo.put("label", cachedTableLabels.getOrDefault(table, table));
            tableInfo.put("fields", new ArrayList<>(
                    cachedFields.getOrDefault(table, Set.of("id", "created_at"))
            ));
            tableList.add(tableInfo);
        }
        return Map.of("tables", tableList);
    }
}
