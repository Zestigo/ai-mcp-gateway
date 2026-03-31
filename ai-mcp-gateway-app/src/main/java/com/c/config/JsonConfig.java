package com.c.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * JSON配置类
 * 用于配置Jackson ObjectMapper实例，定义不同场景下的JSON序列化/反序列化策略
 * 
 * @author cyh
 * @date 2026/03/31
 */
@Configuration
public class JsonConfig {

    /**
     * 全局默认ObjectMapper配置
     * 用于普通API响应，支持下划线命名策略，忽略null值
     * 
     * @return 配置完成的ObjectMapper实例
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                // 忽略未知属性，提高反序列化容错性
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                // 使用下划线命名策略，适配数据库字段命名
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                // 序列化时忽略null值，减少响应数据体积
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * MCP协议专用ObjectMapper配置
     * 强制遵循JSON-RPC规范，使用驼峰命名策略
     * 
     * @return MCP专用的ObjectMapper实例
     */
    @Bean(name = "mcpObjectMapper")
    public ObjectMapper mcpObjectMapper() {
        return new ObjectMapper()
                // 忽略未知属性，提高反序列化容错性
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                // 保持默认命名策略（驼峰），确保满足JSON-RPC规范
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
}