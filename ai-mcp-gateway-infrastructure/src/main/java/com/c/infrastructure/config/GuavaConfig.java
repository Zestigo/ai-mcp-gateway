package com.c.infrastructure.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Guava缓存配置类
 * 功能：配置基于Guava的本地缓存实例，用于临时存储键值对数据
 *
 * @author cyh
 * @date 2026/03/19
 */
@Configuration
public class GuavaConfig {

    /**
     * 创建Guava缓存Bean
     * 命名为"cache"，可通过@Qualifier("cache")精准注入
     * 缓存策略：写入后3秒过期，适用于高频访问、实时性要求高的临时数据
     *
     * @return 配置好的Guava Cache实例
     */
    @Bean(name = "cache")
    public Cache<String, String> cache() {
        return CacheBuilder.newBuilder()
                           // 设置写入后过期时间：3秒
                           .expireAfterWrite(3, TimeUnit.SECONDS)
                           // 构建缓存实例（默认无初始容量和最大容量限制）
                           .build();
    }

}