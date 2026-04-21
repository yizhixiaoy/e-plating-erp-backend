package com.plating.erp.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * 注意：已禁用自动租户拦截器，所有租户过滤由业务层手动控制
 */
@Configuration
public class MybatisPlusConfig {
    private static final Logger log = LoggerFactory.getLogger(MybatisPlusConfig.class);

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 注意：已禁用租户拦截器，所有租户过滤由业务层手动控制
        // 原因：自动拦截在连表查询时会为所有表添加 tenant_id 条件，导致逻辑混乱
        // 解决方案：在 Service 层的 LambdaQueryWrapper 中显式指定 tenant_id 条件
        
        // 分页拦截器
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        
        log.info("MyBatis-Plus 拦截器初始化完成（已禁用自动租户拦截）");
        return interceptor;
    }
}
