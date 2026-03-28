package com.c.infrastructure.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Redis 缓存键统一管理类
 * 采用枚举模块化管理业务键空间，通过通用构建器生成规范缓存键
 * 遵循 mcp_gateway:业务模块:唯一标识 格式，避免键冲突并提升扩展性
 *
 * @author cyh
 * @date 2026/03/27
 */
public class RedisKeyConstants {

    /** 系统全局基础前缀，用于环境与业务隔离 */
    private static final String BASE_PREFIX = "mcp_gateway";

    /** 缓存键层级分隔符 */
    private static final String SEPARATOR = ":";

    /**
     * Redis 业务模块枚举
     * 统一管理系统所有缓存业务类型，包含键前缀与功能描述
     */
    @Getter
    @AllArgsConstructor
    public enum Business {

        /** 鉴权聚合信息缓存模块 */
        AUTH("auth", "鉴权聚合信息"),

        /** 限流计数器缓存模块 */
        LIMIT("ratelimit", "限流计数器"),

        /** 算力报表缓存模块 */
        STMT("statement", "算力报表"),

        /** 分布式锁缓存模块 */
        LOCK("lock", "分布式锁");

        /** 业务键前缀 */
        private final String prefix;

        /** 业务描述说明 */
        private final String desc;
    }

    /**
     * 通用缓存键构建器
     * 支持多级唯一标识，自动拼接全局前缀与业务前缀
     *
     * @param business 业务模块枚举
     * @param args     可变长度唯一标识，支持网关ID、API密钥等多级参数
     * @return 规范格式的Redis缓存键
     */
    public static String buildKey(Business business, Object... args) {
        StringBuilder sb = new StringBuilder(64);
        sb
                .append(BASE_PREFIX)
                .append(SEPARATOR)
                .append(business.getPrefix());

        for (Object arg : args) {
            if (arg != null) {
                sb
                        .append(SEPARATOR)
                        .append(arg);
            }
        }
        return sb.toString();
    }

    /**
     * 构建鉴权聚合信息缓存键
     * 兼容原有调用方式，底层委托通用构建器实现
     *
     * @param gatewayId 网关唯一标识
     * @param apiKey    API访问密钥
     * @return 鉴权模块缓存键
     */
    public static String buildAuthKey(String gatewayId, String apiKey) {
        return buildKey(Business.AUTH, gatewayId, apiKey);
    }

    /**
     * 构建限流计数器缓存键
     * 兼容原有调用方式，底层委托通用构建器实现
     *
     * @param gatewayId 网关唯一标识
     * @param apiKey    API访问密钥
     * @return 限流模块缓存键
     */
    public static String buildRateLimitKey(String gatewayId, String apiKey) {
        return buildKey(Business.LIMIT, gatewayId, apiKey);
    }
}