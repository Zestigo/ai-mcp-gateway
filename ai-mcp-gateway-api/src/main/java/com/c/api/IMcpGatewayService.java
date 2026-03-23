package com.c.api;

import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * MCP网关HTTP接口规范：定义SSE连接建立、指令接收接口
 *
 * @author cyh
 * @date 2026/03/23
 */
public interface IMcpGatewayService {

    /**
     * 建立MCP SSE长连接
     *
     * @param gatewayId 网关唯一标识
     * @return SSE事件流，用于推送实时消息
     */
    Flux<ServerSentEvent<String>> establishSSEConnection(String gatewayId);

    /**
     * 接收MCP指令并触发处理
     *
     * @param gatewayId   网关ID
     * @param sessionId   会话ID
     * @param messageBody 指令消息体（JSON格式）
     * @return 响应式处理结果
     */
    Mono<ResponseEntity<Void>> handleMessage(String gatewayId, String sessionId, String messageBody);
}