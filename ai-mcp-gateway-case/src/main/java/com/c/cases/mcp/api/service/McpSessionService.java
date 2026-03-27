package com.c.cases.mcp.api.service;

import com.c.cases.mcp.api.model.McpSessionRequest;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * MCP会话服务API接口
 * 定义会话创建、关闭、SSE流管理能力
 *
 * @author cyh
 * @date 2026/03/27
 */
public interface McpSessionService {

    /**
     * 创建MCP会话并返回SSE事件流（简易模式）
     *
     * @param gatewayId 网关唯一标识
     * @return SSE事件流
     */
    Flux<ServerSentEvent<String>> createMcpSession(String gatewayId);

    /**
     * 创建MCP会话并返回SSE事件流（完整模式）
     *
     * @param request 会话创建请求
     * @return SSE事件流
     */
    Flux<ServerSentEvent<String>> createMcpSession(McpSessionRequest request);

    /**
     * 显式关闭会话并释放资源
     *
     * @param sessionId 会话唯一标识
     * @return 关闭完成异步信号
     */
    Mono<Void> closeSession(String sessionId);
}