package com.c.infrastructure.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Redis键定义枚举
 * 统一管理Redis Key前缀、过期时间、业务描述
 * 自动拼接项目名+环境，实现多环境隔离
 *
 * @author cyh
 * @date 2026/03/31
 */
@Getter
@AllArgsConstructor
public enum RedisKey {

    // ===================== 网关模块 =====================
    /** 网关配置缓存 */
    GW_CONFIG("gw:config", Duration.ofHours(1), "网关配置"),
    /** 网关工具缓存 */
    GW_TOOL("gw:tool", Duration.ofHours(1), "网关工具"),
    /** 网关鉴权列表缓存 */
    GW_AUTH_LIST("gw:auth", Duration.ofHours(1), "网关鉴权列表"),
    /** API密钥缓存 */
    GW_API_KEY("gw:apikey", Duration.ofHours(1), "API密钥"),

    // ===================== 认证模块 =====================
    /** 授权信息缓存 */
    AUTH("auth", Duration.ofHours(2), "授权信息"),
    /** 限流计数器缓存 */
    RATE_LIMIT("ratelimit", Duration.ofMinutes(1), "限流"),
    /** 分布式锁缓存 */
    LOCK("lock", Duration.ofSeconds(30), "分布式锁"),
    /** 报表数据缓存 */
    STATEMENT("stmt", Duration.ofDays(7), "报表");

    // ===================== 固定结构 =====================
    /** 项目统一前缀 */
    private static final String PROJECT = "mcp_gateway";
    /** 运行环境，默认default */
    private static final String ENV = System.getProperty("spring.profiles.active", "default");
    /** Key分隔符 */
    private static final String SEPARATOR = ":";

    /** Key业务前缀 */
    private final String prefix;
    /** 默认过期时间 */
    private final Duration ttl;
    /** 业务描述 */
    private final String desc;

    // ===================== 1. 默认用法（使用枚举定义的时间） =====================

    /**
     * 构建带默认过期时间的Key定义
     *
     * @param args 动态参数
     * @return Key定义对象（包含key+ttl）
     */
    public KeyDefinition build(Object... args) {
        String key = buildKey(args);
        return new KeyDefinition(key, this.ttl);
    }

    // ===================== 2. 自定义时间用法 =====================

    /**
     * 构建带自定义过期时间的Key定义
     *
     * @param customTtl 自定义过期时间
     * @param args      动态参数
     * @return Key定义对象
     */
    public KeyDefinition build(Duration customTtl, Object... args) {
        String key = buildKey(args);
        return new KeyDefinition(key, customTtl);
    }

    // ===================== 3. 只拿 key（不关心时间） =====================

    /**
     * 仅获取拼接后的Redis Key
     *
     * @param args 动态参数
     * @return 完整Key字符串
     */
    public String getKey(Object... args) {
        return buildKey(args);
    }

    // ===================== 内部拼接 key =====================

    /**
     * 内部方法：拼接完整Redis Key
     *
     * @param args 动态参数
     * @return 标准格式Key
     */
    private String buildKey(Object... args) {
        StringBuilder sb = new StringBuilder(128);
        sb
                .append(PROJECT)
                .append(SEPARATOR)
                .append(ENV)
                .append(SEPARATOR)
                .append(prefix);
        for (Object arg : args) {
            if (arg != null && StringUtils.hasText(arg.toString())) {
                sb
                        .append(SEPARATOR)
                        .append(arg);
            }
        }
        return sb.toString();
    }

    /**
     * Redis Key定义内部类
     * 封装完整Key和过期时间
     */
    @Getter
    @AllArgsConstructor
    public static class KeyDefinition {
        /** 完整Redis Key */
        private final String key;
        /** 过期时间 */
        private final Duration ttl;
    }
}