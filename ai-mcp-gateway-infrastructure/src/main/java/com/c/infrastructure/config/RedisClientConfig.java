package com.c.infrastructure.config;

import com.c.infrastructure.config.properties.RedisClientConfigProperties;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.support.ResourceScriptSource;

import javax.annotation.Resource;

/**
 * Redis 客户端统一配置类
 * 负责 Redisson 客户端、RedisTemplate、Lua 脚本、序列化机制的全局配置
 * 统一序列化方案，保证本地缓存、Redis、数据库三层数据结构一致
 *
 * @author cyh
 * @date 2026/03/28
 */
@Configuration
@EnableConfigurationProperties(RedisClientConfigProperties.class)
public class RedisClientConfig {

    /** Redis 配置属性类，加载配置文件中的 Redis 连接参数 */
    @Resource
    private RedisClientConfigProperties properties;

    /**
     * 创建全局统一的 ObjectMapper 序列化配置
     * 解决 Java8 时间类型、泛型丢失、多态反序列化、VO 对象转换问题
     *
     * @return 配置完成的 ObjectMapper
     */
    private ObjectMapper createObjectMapper() {
        // 初始化 ObjectMapper 实例
        ObjectMapper om = new ObjectMapper();
        // 设置所有属性可见，支持任意访问权限
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 注册 Java8 时间模块，支持 LocalDateTime 等时间类型序列化
        om.registerModule(new JavaTimeModule());
        // 激活默认类型推导，解决反序列化时 LinkedHashMap 无法强转为目标 VO 问题
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return om;
    }

    /**
     * 初始化 Redisson 客户端
     * 使用 JsonJacksonCodec 统一序列化，支持单机模式全配置项加载
     *
     * @return RedissonClient 分布式 Redis 客户端
     */
    @Bean(name = "redissonClient")
    public RedissonClient redissonClient() {
        // 创建 Redisson 配置对象
        Config config = new Config();
        // 设置与系统统一的 JSON 序列化器
        config.setCodec(new JsonJacksonCodec(createObjectMapper()));

        // 单机模式配置，加载配置文件中的连接、池化、超时、保活等参数
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

        // 创建并返回 Redisson 客户端实例
        return Redisson.create(config);
    }

    /**
     * 配置全局 RedisTemplate
     * Key 采用字符串序列化，Value 采用统一 Jackson 序列化，与 Redisson 保持兼容
     *
     * @param factory Redis 连接工厂
     * @return 配置完成的 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        // 创建 RedisTemplate 实例
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 设置连接工厂
        template.setConnectionFactory(factory);

        // 使用统一 Jackson 序列化器
        ObjectMapper om = createObjectMapper();
        Jackson2JsonRedisSerializer<Object> jacksonSerializer = new Jackson2JsonRedisSerializer<>(om, Object.class);

        // Key / HashKey 使用字符串序列化
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        // Value / HashValue 使用 Jackson 序列化
        template.setValueSerializer(jacksonSerializer);
        template.setHashValueSerializer(jacksonSerializer);

        // 完成属性装配初始化
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 预加载分布式限流 Lua 脚本
     * 从 classpath:com/c/infrastructure/resources/ratelimit.lua 加载脚本，确保原子性限流
     *
     * @return 可执行的 Redis Lua 脚本
     */
    @Bean
    public DefaultRedisScript<Long> ratelimitLuaScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // 设置返回值类型为 Long
        script.setResultType(Long.class);
        // 加载 Lua 脚本资源
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("com/c/infrastructure/resources" +
                "/ratelimit.lua")));
        return script;
    }
}