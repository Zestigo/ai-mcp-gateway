package com.c.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.text.SimpleDateFormat;

@Configuration
public class JsonConfig {
    @Bean
    @Primary // 确保这个是默认的“最高指挥官”
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            // 基础配置：忽略未知属性，防止报错
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            // 关键：统一命名策略（如果下游全是下划线，这里一键配置）
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            // 统一日期格式
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
            // 不序列化 null 字段
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
}