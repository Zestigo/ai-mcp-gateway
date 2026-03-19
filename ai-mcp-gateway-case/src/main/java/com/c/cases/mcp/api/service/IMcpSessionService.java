package com.c.cases.mcp.api.service;

import com.c.cases.mcp.api.model.McpSessionRequest;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * MCP会话管理服务接口：维护与客户端的响应式SSE通信连接
 *
 * @author cyh
 * @date 2026/03/19
 */
public interface IMcpSessionService {

    /**
     * 创建MCP会话连接：返回包含MCP协议指令的SSE流
     *
     * @param gatewayId 网关唯一标识
     * @return SSE事件流（客户端断连自动关流释放资源，错误通过Flux error信号传递）
     */
    Flux<ServerSentEvent<String>> createMcpSession(String gatewayId);

    /**
     * 创建MCP会话连接（重载方法）
     *
     * @param request 会话请求对象
     * @return SSE事件流
     */
    Flux<ServerSentEvent<String>> createMcpSession(McpSessionRequest request);

}