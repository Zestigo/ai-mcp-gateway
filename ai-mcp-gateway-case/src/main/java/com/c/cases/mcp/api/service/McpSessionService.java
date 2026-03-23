package com.c.cases.mcp.api.service;

import com.c.cases.mcp.api.model.McpSessionRequest;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * MCP 会话服务接口
 * 定义会话创建、消息推送等核心业务标准接口
 *
 * @author cyh
 * @date 2026/03/24
 */
public interface McpSessionService {

    /**
     * 根据网关ID创建MCP会话并建立SSE长连接
     *
     * @param gatewayId 网关唯一标识
     * @return SSE事件流对象
     */
    Flux<ServerSentEvent<String>> createMcpSession(String gatewayId);

    /**
     * 根据完整请求参数创建MCP会话并建立SSE长连接
     *
     * @param request 会话创建请求对象
     * @return SSE事件流对象
     */
    Flux<ServerSentEvent<String>> createMcpSession(McpSessionRequest request);

    /**
     * 向指定会话推送消息
     *
     * @param sessionId 会话唯一标识
     * @param message   待推送的消息内容
     */
    void pushMessage(String sessionId, Object message);
}