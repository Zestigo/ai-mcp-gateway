package com.c.domain.session.model.entity;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

/**
 * 会话实体
 *
 * @author cyh
 * @date 2026/03/24
 */
@Getter
public class McpSession {

    /** 默认超时时间（秒） */
    private static final int DEFAULT_TIMEOUT_SECONDS = 1800;

    /** 会话唯一标识 */
    private final String sessionId;

    /** 网关唯一标识 */
    private final String gatewayId;

    /** 会话超时时间（秒） */
    private final Integer timeoutSeconds;

    /** 会话创建时间 */
    private final Instant createTime;

    /** 最后访问时间 */
    private volatile Instant lastAccessTime;

    /** 会话是否有效 */
    private volatile boolean active = true;

    /**
     * 构造方法
     *
     * @param sessionId      会话唯一标识
     * @param gatewayId      网关唯一标识
     * @param timeoutSeconds 超时时间
     */
    public McpSession(String sessionId, String gatewayId, Integer timeoutSeconds) {
        this.sessionId = sessionId;
        this.gatewayId = gatewayId;
        // 超时时间为空或小于0时，使用默认超时时间
        this.timeoutSeconds = (timeoutSeconds == null || timeoutSeconds <= 0) ? DEFAULT_TIMEOUT_SECONDS :
                timeoutSeconds;
        this.createTime = Instant.now();
        // 初始化最后访问时间为创建时间
        this.lastAccessTime = this.createTime;
    }

    /**
     * 构造方法（使用默认超时）
     *
     * @param sessionId 会话唯一标识
     * @param gatewayId 网关唯一标识
     */
    public McpSession(String sessionId, String gatewayId) {
        this(sessionId, gatewayId, null);
    }

    /**
     * 更新最后访问时间
     */
    public void touch() {
        // 刷新会话活跃时间，避免过期
        this.lastAccessTime = Instant.now();
    }

    /**
     * 将会话标记为无效
     */
    public void deactivate() {
        // 标记会话为失效状态
        this.active = false;
    }

    /**
     * 判断会话是否过期
     *
     * @return 过期返回 true
     */
    public boolean isExpired() {
        // 当前时间减去超时时间，若最后访问时间更早则判定过期
        return lastAccessTime.isBefore(Instant
                .now()
                .minus(Duration.ofSeconds(timeoutSeconds)));
    }
}