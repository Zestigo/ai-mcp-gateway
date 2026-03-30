package com.c.api.service;

import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * MCP 网关服务接口
 *
 * @author cyh
 * @date 2026/03/30
 */
public interface McpGatewayService {

    /**
     * 建立 SSE 长连接
     * 对应路径: /api/v1/gateways/{gatewayId}/sse
     *
     * @param gatewayId    路径变量：网关唯一标识
     * @param headerApiKey Header参数：X-Api-Key
     * @param queryApiKey  Query参数：api_key
     * @return SSE 事件流
     */
    Flux<ServerSentEvent<String>> establishSSEConnection(String gatewayId, String headerApiKey, String queryApiKey);

    /**
     * 处理 MCP 消息
     * 对应路径: /api/v1/gateways/{gatewayId}/sessions/{sessionId}/messages
     *
     * @param gatewayId    路径变量：网关唯一标识
     * @param sessionId    路径变量：会话唯一标识
     * @param headerApiKey Header参数：X-Api-Key
     * @param queryApiKey  Query参数：api_key
     * @param body         请求体：JSON-RPC 报文
     * @return 处理结果响应
     */
    Mono<ResponseEntity<Void>> handleMessage(String gatewayId, String sessionId, String headerApiKey,
                                             String queryApiKey, String body);
}