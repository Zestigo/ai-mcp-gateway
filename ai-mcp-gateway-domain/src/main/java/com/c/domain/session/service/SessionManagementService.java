package com.c.domain.session.service;

import com.c.domain.session.model.entity.McpSession;
import reactor.core.publisher.Mono;

/**
 * 会话管理领域服务接口
 *
 * @author cyh
 * @date 2026/03/24
 */
public interface SessionManagementService {

    /**
     * 创建会话
     * @param gatewayId 网关标识
     * @return 会话实体
     */
    Mono<McpSession> createSession(String gatewayId);

    /**
     * 创建会话（指定超时）
     * @param gatewayId 网关标识
     * @param timeoutSeconds 超时时间
     * @return 会话实体
     */
    Mono<McpSession> createSession(String gatewayId, Integer timeoutSeconds);

    /**
     * 获取会话
     * @param sessionId 会话标识
     * @return 会话实体
     */
    Mono<McpSession> getSession(String sessionId);

    /**
     * 移除会话
     * @param sessionId 会话标识
     * @return 执行结果
     */
    Mono<Void> removeSession(String sessionId);

    /**
     * 清理过期会话
     * @return 执行结果
     */
    Mono<Void> cleanupExpiredSessions();

    /**
     * 关闭服务
     * @return 执行结果
     */
    Mono<Void> shutdown();
}