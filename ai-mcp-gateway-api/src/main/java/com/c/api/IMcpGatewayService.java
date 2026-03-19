package com.c.api;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * MCP 网关服务接口
 * 提供基于 SSE (Server-Sent Events) 的流式通信能力
 *
 * @author cyh
 * @date 2026/03/18
 */
public interface IMcpGatewayService {

    /**
     * 建立 SSE 连接
     *
     * @param gatewayId 网关唯一标识 ID
     * @return 响应式流，推送 ServerSentEvent 格式的消息
     */
    Flux<ServerSentEvent<String>> establishSSEConnection(String gatewayId);

}