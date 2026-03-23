package com.c.cases.mcp.api.service;

import com.c.cases.mcp.api.model.McpSessionRequest;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * MCP会话管理接口
 * 定义SSE会话创建、消息推送的核心规范
 *
 * @author cyh
 * @date 2026/03/19
 */
public interface IMcpSessionService {

    /**
     * 创建MCP SSE长连接会话
     * 客户端断连时流自动终止，触发会话资源释放
     *
     * @param gatewayId 网关唯一标识
     * @return SSE事件流，持续推送MCP协议消息
     */
    Flux<ServerSentEvent<String>> createMcpSession(String gatewayId);

    /**
     * 创建MCP会话（重载扩展）
     * 适配复杂请求参数场景
     *
     * @param request 会话请求对象
     * @return SSE事件流
     */
    Flux<ServerSentEvent<String>> createMcpSession(McpSessionRequest request);

    /**
     * 向指定会话推送消息（服务端主动推送）
     *
     * @param sessionId 会话唯一标识
     * @param message   业务消息对象（MCP协议格式）
     */
    void pushMessage(String sessionId, Object message);
}