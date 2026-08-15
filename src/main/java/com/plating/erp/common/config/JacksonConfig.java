package com.plating.erp.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局配置
 * 1. 将 Long 类型序列化为 String，防止前端 JS 精度丢失（雪花算法ID为19位数字，超过Number.MAX_SAFE_INTEGER）
 * 2. 注册 JavaTimeModule，支持 Java 8 时间类型（LocalDateTime 等）序列化
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            // Long -> String
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            // 注册 JavaTimeModule 支持 LocalDateTime 序列化，并保留 Long→String 的自定义模块
            builder.modules(new JavaTimeModule(), module);
        };
    }
}
