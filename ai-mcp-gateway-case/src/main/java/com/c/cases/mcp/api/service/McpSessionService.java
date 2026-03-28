package com.c.cases.mcp.api.service;

import com.c.cases.mcp.api.model.McpSessionRequest;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * MCP会话服务API接口
 * 定义会话生命周期管理标准接口，包含创建与关闭能力
 *
 * @author cyh
 * @date 2026/03/28
 */
public interface McpSessionService {

    /**
     * 创建MCP会话并返回SSE事件流（简易模式）
     * 简化参数调用，适用于基础场景
     *
     * @param gatewayId 网关唯一标识
     * @param apiKey    接口访问密钥
     * @return SSE事件流，用于前端长连接
     */
    Flux<ServerSentEvent<String>> createMcpSession(String gatewayId, String apiKey);

    /**
     * 创建MCP会话并返回SSE事件流（完整模式）
     * 支持扩展请求参数，适用于复杂场景
     *
     * @param request 会话创建请求对象
     * @return SSE事件流
     */
    Flux<ServerSentEvent<String>> createMcpSession(McpSessionRequest request);

    /**
     * 显式关闭会话并释放资源
     * 主动终止连接并清理会话数据
     *
     * @param sessionId 会话唯一标识
     * @return 异步关闭完成信号
     */
    Mono<Void> closeSession(String sessionId);
}