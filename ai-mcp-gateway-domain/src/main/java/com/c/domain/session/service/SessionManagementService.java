package com.c.domain.session.service;

import com.c.domain.session.model.entity.McpSession;
import reactor.core.publisher.Mono;

/**
 * MCP 会话管理服务接口
 * 负责会话的创建、获取、续期、移除、过期清理及服务关闭
 */
public interface SessionManagementService {

    /**
     * 创建会话（使用默认超时时间）
     *
     * @param gatewayId 网关唯一标识
     * @return 会话实体
     */
    Mono<McpSession> createSession(String gatewayId);

    /**
     * 创建会话（支持自定义超时时间）
     *
     * @param gatewayId      网关唯一标识
     * @param timeoutSeconds 会话超时秒数
     * @return 会话实体
     */
    Mono<McpSession> createSession(String gatewayId, Integer timeoutSeconds);

    /**
     * 获取会话并自动续期
     * 若会话已过期或失效，会自动清理并返回空
     *
     * @param sessionId 会话唯一标识
     * @return 会话实体
     */
    Mono<McpSession> getSession(String sessionId);

    /**
     * 主动移除并销毁会话
     *
     * @param sessionId 会话唯一标识
     * @return 执行结果
     */
    Mono<Void> removeSession(String sessionId);

    /**
     * 清理数据库中已过期的会话
     *
     * @return 执行结果
     */
    Mono<Void> cleanupExpiredSessions();

    /**
     * 关闭会话管理服务，释放资源
     *
     * @return 执行结果
     */
    Mono<Void> shutdown();
}