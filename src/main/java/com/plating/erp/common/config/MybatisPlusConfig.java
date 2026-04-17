package com.plating.erp.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.plating.erp.common.security.CurrentUser;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.common.tenant.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * MyBatis-Plus 配置类
 * 
 * 配置多租户拦截器，实现行级租户数据隔离：
 * - 平台用户（userType=0）：跳过租户隔离，可访问所有租户数据
 * - 租户用户（userType=1）：强制添加 tenant_id 条件，只能访问本租户数据
 * - 未登录请求：返回 -1L 防止数据泄露
 * 
 * 白名单表（不需要租户隔离）：
 * - sys_tenant：租户表，平台级数据
 * - sys_menu：菜单模板表，平台级数据
 * - sys_plan：套餐表，平台级数据
 * - flyway_schema_history：迁移历史表
 */
@Configuration
public class MybatisPlusConfig {
    private static final Logger log = LoggerFactory.getLogger(MybatisPlusConfig.class);
    
    /**
     * 租户隔离白名单表（平台级表，不需要 tenant_id 隔离）
     */
    private static final Set<String> TENANT_WHITE_TABLES = Set.of(
            "sys_tenant",
            "sys_menu",
            "sys_plan",
            "flyway_schema_history"
    );

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor(new CustomTenantLineHandler());
        interceptor.addInnerInterceptor(tenantInterceptor);
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
    
    /**
     * 自定义租户拦截器处理器
     * 
     * 核心逻辑：
     * 1. 白名单表 → 跳过租户隔离
     * 2. 平台用户 → 跳过租户隔离
     * 3. 租户用户 → 使用 TenantContextHolder 中的租户ID
     * 4. 未登录 → 返回 -1L 安全兜底
     */
    private class CustomTenantLineHandler implements TenantLineHandler {
        
        /**
         * 获取租户ID值
         * 
         * @return 租户ID表达式，返回 null 表示跳过租户条件
         */
        @Override
        public Expression getTenantId() {
            // 1. 尝试获取当前用户信息
            CurrentUser currentUser = null;
            try {
                currentUser = SecurityUtils.currentUser();
            } catch (Exception e) {
                // 未登录，继续使用 TenantContextHolder
                log.debug("未获取到用户信息，尝试从 TenantContextHolder 获取租户ID");
            }
            
            // 2. 平台用户跳过租户隔离（返回 null 表示不添加租户条件）
            if (currentUser != null && currentUser.isSystem()) {
                log.debug("平台用户跳过租户隔离, userId={}, userType={}", 
                        currentUser.userId(), currentUser.userType());
                return null;  // 返回 null 跳过租户条件
            }
            
            // 3. 租户用户或未登录请求，从 TenantContextHolder 获取租户ID
            Long tenantId = TenantContextHolder.getTenantId();
            if (tenantId != null) {
                log.debug("使用租户上下文, tenantId={}", tenantId);
                return new LongValue(tenantId);
            }
            
            // 4. 无租户上下文，返回 -1L 安全兜底（查不到任何数据）
            log.warn("租户上下文为空，使用 -1L 防止数据泄露");
            return new LongValue(-1L);
        }

        @Override
        public String getTenantIdColumn() {
            return "tenant_id";
        }

        /**
         * 判断是否忽略表的租户隔离
         * 
         * 规则：
         * 1. 白名单表 → 跳过
         * 2. 平台用户 → 跳过（可访问所有租户数据）
         * 3. 其他情况 → 不跳过
         */
        @Override
        public boolean ignoreTable(String tableName) {
            // 白名单表跳过
            if (TENANT_WHITE_TABLES.contains(tableName)) {
                return true;
            }
            
            // 平台用户跳过所有表的租户隔离
            try {
                CurrentUser currentUser = SecurityUtils.currentUser();
                if (currentUser.isSystem()) {
                    return true;
                }
            } catch (Exception e) {
                // 未登录，不跳过
            }
            
            return false;
        }
    }
}
