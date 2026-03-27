package com.c.domain.session.model.entity;

import lombok.*;

import java.time.Duration;
import java.time.Instant;

/**
 * MCP会话领域实体
 * 存储会话核心属性与生命周期状态，提供会话活跃校验、过期判断、续期能力
 *
 * @author cyh
 * @date 2026/03/27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpSession {

    /** 默认会话超时时间，单位：秒，默认30分钟 */
    private static final int DEFAULT_TIMEOUT_SECONDS = 1800;

    /** 会话唯一标识，全局不可重复 */
    private String sessionId;

    /** 网关唯一标识，标识会话所属网关 */
    private String gatewayId;

    /** 会话所在宿主机IP，用于分布式跨节点消息路由 */
    private String hostIp;

    /** 会话超时时间，为空使用默认值 */
    @Builder.Default
    private Integer timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

    /** 会话创建时间，创建时自动初始化 */
    @Builder.Default
    private Instant createTime = Instant.now();

    /** 会话最后访问时间，volatile保证多线程可见性 */
    @Builder.Default
    private volatile Instant lastAccessTime = Instant.now();

    /** 会话活跃状态，true-活跃 false-失效，volatile保证多线程可见性 */
    @Builder.Default
    private volatile boolean active = true;

    /**
     * 刷新会话最后访问时间
     * 用于会话心跳、请求处理时更新活跃时间
     */
    public void touch() {
        this.lastAccessTime = Instant.now();
    }

    /**
     * 判断会话是否已过期
     * 基于最后访问时间+超时时间与当前时间对比
     *
     * @return true-已过期 false-未过期
     */
    public boolean isExpired() {
        int timeout = (timeoutSeconds == null || timeoutSeconds <= 0) ? DEFAULT_TIMEOUT_SECONDS : timeoutSeconds;
        return Instant
                .now()
                .isAfter(lastAccessTime.plus(Duration.ofSeconds(timeout)));
    }

    /**
     * 获取会话活跃状态数值
     * 用于MyBatis持久化映射，布尔值转换为数字
     *
     * @return 1-活跃 0-失效
     */
    public int getActiveState() {
        return active ? 1 : 0;
    }
}