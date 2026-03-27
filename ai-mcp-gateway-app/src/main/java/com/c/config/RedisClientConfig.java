package com.c.config;

import com.c.config.properties.RedisClientConfigProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * Redis客户端配置类
 * 基于Redisson实现Redis连接管理，使用JsonJacksonCodec解决序列化与反序列化问题
 * 支持嵌套泛型、多态类型自动解析，替代Fastjson避免数据还原失败
 *
 * @author cyh
 * @date 2026/03/27
 */
@Configuration
@EnableConfigurationProperties(RedisClientConfigProperties.class)
public class RedisClientConfig {

    /** Redis配置属性 */
    @Resource
    private RedisClientConfigProperties properties;

    /**
     * 构建Redisson客户端实例
     * 采用单节点模式，配置连接池、超时、心跳等参数
     * 使用JsonJacksonCodec保证复杂对象序列化稳定性
     *
     * @param applicationContext 应用上下文
     * @return Redisson客户端实例
     */
    @Bean("redissonClient")
    public RedissonClient redissonClient(ConfigurableApplicationContext applicationContext) {
        Config config = new Config();

        // 使用Jackson编解码器，解决嵌套泛型、多态类型序列化失败问题
        config.setCodec(new JsonJacksonCodec());

        // 单节点Redis配置
        config
                .useSingleServer()
                .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
                .setPassword(properties.getPassword())
                .setConnectionPoolSize(properties.getPoolSize())
                .setConnectionMinimumIdleSize(properties.getMinIdleSize())
                .setIdleConnectionTimeout(properties.getIdleTimeout())
                .setConnectTimeout(properties.getConnectTimeout())
                .setRetryAttempts(properties.getRetryAttempts())
                .setRetryInterval(properties.getRetryInterval())
                .setPingConnectionInterval(properties.getPingInterval())
                .setKeepAlive(properties.isKeepAlive());

        return Redisson.create(config);
    }
}