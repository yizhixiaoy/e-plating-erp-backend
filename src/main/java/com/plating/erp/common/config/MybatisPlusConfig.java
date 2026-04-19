package com.plating.erp.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.plating.erp.common.security.CurrentUser;
import com.plating.erp.common.security.SecurityUtils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

/**
 * MyBatis-Plus 多租户配置
 *
 * 
 * 核心设计原则：
 * 1. 租户ID优先从 SecurityContext 获取（JWT Token 中的 tenantId）
 * 2. 平台用户（userType=0）完全跳过租户隔离
 * 3. 白名单表通过配置文件管理，支持动态扩展
 * 4. 无租户上下文时返回 null（不拼接条件），由业务层通过 @InterceptorIgnore 控制
 */
@Configuration
public class MybatisPlusConfig {
    private static final Logger log = LoggerFactory.getLogger(MybatisPlusConfig.class);
    
    /**
     * 租户隔离白名单表
     * 
     * 分类说明：
     * 1. 平台级配置表：租户管理、菜单模板、套餐等
     * 2. 认证相关表：登录历史、用户最近租户（登录时上下文不完整）
     * 3. 系统级表：Flyway 迁移历史
     * 
     * 注意：新增白名单表时，需在此处添加并说明原因
     */
    private static final Set<String> TENANT_WHITE_TABLES = new HashSet<>() {{
        // 平台级配置表
        add("sys_tenant");              // 租户信息表
        add("sys_menu");                // 菜单模板表
        add("sys_plan");                // 套餐表
        
        // 认证相关表（登录/注册时租户上下文不完整）
        add("sys_login_history");       // 登录历史记录
        add("sys_user_recent_tenant");  // 用户最近登录租户
        
        // 系统级表
        add("flyway_schema_history");   // 数据库迁移历史
    }};

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 租户拦截器（必须在分页拦截器之前）
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            
            @Override
            public Expression getTenantId() {
                // 优先从 SecurityContext 获取租户ID
                try {
                    CurrentUser user = SecurityUtils.currentUser();
                    
                    // 平台用户已在 ignoreTable 中处理，这里只处理租户用户
                    if (!user.isSystem() && user.tenantId() != null) {
                        return new LongValue(user.tenantId());
                    }
                } catch (Exception e) {
                    // 未登录或 Token 无效
                    log.debug("未获取到用户信息，跳过租户条件");
                }
                
                // 无租户上下文，返回 null（不拼接条件）
                // 注意：白名单表和无上下文场景已在 ignoreTable 中处理
                return null;
            }
            
            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }
            
            @Override
            public boolean ignoreTable(String tableName) {
                // 1. 白名单表跳过租户隔离（平台级表、系统级表）
                if (TENANT_WHITE_TABLES.contains(tableName)) {
                    return true;
                }
                
                // 2. 平台用户跳过所有表的租户隔离
                try {
                    CurrentUser user = SecurityUtils.currentUser();
                    if (user.isSystem()) {
                        return true;
                    }
                } catch (Exception e) {
                    // 3. 未登录场景：不跳过租户隔离，返回 false
                    // 原因：
                    // - 公开接口（/auth/**）已在 ignoreTable 白名单中处理
                    // - 需要认证的接口未登录时应该拦截（由 Spring Security 处理）
                    // - 如果这里返回 true，会导致未登录请求能查询所有租户数据（安全漏洞）
                    log.debug("未登录场景，不跳过表 [{}] 的租户隔离（由 Spring Security 控制）", tableName);
                    return false;
                }
                
                return false;
            }
        }));
        
        // 分页拦截器
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        
        log.info("MyBatis-Plus 多租户拦截器初始化完成, 白名单表数量={}", TENANT_WHITE_TABLES.size());
        return interceptor;
    }
}
