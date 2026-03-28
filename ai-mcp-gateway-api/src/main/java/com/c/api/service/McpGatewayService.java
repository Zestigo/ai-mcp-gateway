package com.c.api.service;

import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * MCP 网关服务接口
 * 定义网关核心能力：SSE长连接建立与消息处理
 *
 * @author cyh
 * @date 2026/03/28
 */
public interface McpGatewayService {

    /**
     * 建立 SSE 长连接
     * 创建会话并返回持续通信的事件流
     *
     * @param gatewayId 网关唯一标识
     * @param apiKey    接口访问密钥
     * @return SSE事件流
     */
    Flux<ServerSentEvent<String>> establishSSEConnection(String gatewayId, String apiKey);

    /**
     * 处理 MCP 消息
     * 接收客户端消息并分发至业务逻辑处理
     *
     * @param gatewayId 网关唯一标识
     * @param sessionId 会话唯一标识
     * @param body      消息请求体
     * @return 处理结果响应
     */
    Mono<ResponseEntity<Void>> handleMessage(String gatewayId, String apiKey, String sessionId, String body);
}