package com.c.api;

import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * MCP 网关服务接口
 * 核心能力：提供基于 SSE（Server-Sent Events，服务器推送事件）的流式通信能力
 * 主要用于网关与客户端之间的实时消息交互
 *
 * @author cyh
 * @date 2026/03/18
 */
public interface IMcpGatewayService {

    /**
     * 建立 SSE 长连接
     * 用于网关与客户端维持实时的单向消息推送通道
     *
     * @param gatewayId 网关唯一标识ID，用于区分不同的网关实例
     * @return 响应式数据流，持续推送 ServerSentEvent 格式的字符串消息
     */
    Flux<ServerSentEvent<String>> establishSSEConnection(String gatewayId);

    /**
     * 处理网关上报的消息
     * 接收网关推送的业务消息并完成相应的业务处理
     *
     * @param sessionId   会话ID，用于标识网关与客户端的单次通信会话
     * @param messageBody 消息体内容，为具体的业务消息字符串
     * @return 响应式的请求处理结果，包含处理状态和响应数据
     */
    Mono<ResponseEntity<Object>> handleMessage(String sessionId, String messageBody);
}