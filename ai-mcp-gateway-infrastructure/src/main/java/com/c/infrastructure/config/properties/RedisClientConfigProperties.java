package com.c.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 客户端连接配置属性
 * 从配置文件加载 Redis 连接、连接池、超时、重试等相关参数
 *
 * @author cyh
 * @date 2026/03/28
 */
@Data
@ConfigurationProperties(prefix = "redis.sdk.config")
public class RedisClientConfigProperties {

    /** Redis 服务地址 */
    private String host;

    /** Redis 服务端口 */
    private int port;

    /** Redis 访问密码，无密码时可为空 */
    private String password;

    /** 最大连接池大小 */
    private int poolSize = 64;

    /** 最小空闲连接数 */
    private int minIdleSize = 10;

    /** 空闲连接超时时间，单位：毫秒 */
    private int idleTimeout = 10000;

    /** 连接超时时间，单位：毫秒 */
    private int connectTimeout = 10000;

    /** 连接重试次数 */
    private int retryAttempts = 3;

    /** 重试间隔时间，单位：毫秒 */
    private int retryInterval = 1000;

    /** 心跳检测间隔，单位：毫秒，0 表示不开启 */
    private int pingInterval = 0;

    /** 是否启用长连接保持 */
    private boolean keepAlive = true;
}