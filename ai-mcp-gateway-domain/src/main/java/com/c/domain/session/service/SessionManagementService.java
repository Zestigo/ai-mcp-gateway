package com.c.domain.session.service;

import com.c.domain.session.model.entity.McpSession;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Optional;
import java.util.Set;

/**
 * 会话管理服务接口
 * 定义MCP会话创建、查询、销毁、续期、清理及本地SSE连接管理能力
 *
 * @author cyh
 * @date 2026/03/27
 */
public interface SessionManagementService {

    /**
     * 创建会话（使用默认超时时间）
     *
     * @param gatewayId 网关唯一标识
     * @return 新创建的会话实体
     */
    Mono<McpSession> createSession(String gatewayId);

    /**
     * 创建会话（支持自定义超时时间）
     *
     * @param gatewayId      网关唯一标识
     * @param timeoutSeconds 会话超时时间，单位秒
     * @return 新创建的会话实体
     */
    Mono<McpSession> createSession(String gatewayId, Integer timeoutSeconds);

    /**
     * 获取会话并自动续期
     * 若会话已过期或失效，自动清理并返回空
     *
     * @param sessionId 会话唯一标识
     * @return 有效会话实体
     */
    Mono<McpSession> getSession(String sessionId);

    /**
     * 主动销毁会话，关闭SSE连接并删除数据
     *
     * @param sessionId 会话唯一标识
     * @return 销毁完成异步信号
     */
    Mono<Void> removeSession(String sessionId);

    /**
     * 清理数据库中已过期的会话
     *
     * @return 清理完成异步信号
     */
    Mono<Void> cleanupExpiredSessions();

    /**
     * 获取本地SSE消息推送Sink
     *
     * @param sessionId 会话唯一标识
     * @return 本地Sink包装对象
     */
    Optional<Sinks.Many<ServerSentEvent<String>>> getLocalSink(String sessionId);

    /**
     * 注册本地SSE消息推送Sink
     *
     * @param sessionId 会话唯一标识
     * @param sink      SSE推送句柄
     */
    void registerSink(String sessionId, Sinks.Many<ServerSentEvent<String>> sink);

    /**
     * 服务优雅关闭，释放所有资源
     *
     * @return 关闭完成异步信号
     */
    Mono<Void> shutdown();

    /**
     * 获取当前节点所有本地会话ID
     *
     * @return 本地会话ID集合
     */
    Set<String> getAllLocalKeys();
}