package com.c.api.service;

import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * MCP 网关服务接口
 *
 * @author cyh
 * @date 2026/03/24
 */
public interface McpGatewayService {

    /**
     * 建立 SSE 长连接
     *
     * @param gatewayId 网关唯一标识
     * @return SSE 事件流
     */
    Flux<ServerSentEvent<String>> establishSSEConnection(String gatewayId);

    /**
     * 处理 MCP 消息
     *
     * @param gatewayId 网关唯一标识
     * @param sessionId 会话唯一标识
     * @param body      请求体内容
     * @return 响应实体
     */
    Mono<ResponseEntity<Void>> handleMessage(String gatewayId, String sessionId, String body);
}