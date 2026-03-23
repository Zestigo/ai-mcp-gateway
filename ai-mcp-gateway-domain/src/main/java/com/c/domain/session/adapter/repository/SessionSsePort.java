package com.c.domain.session.adapter.repository;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

/**
 * 会话 SSE 连接存储接口
 *
 * @author cyh
 * @date 2026/03/24
 */
public interface SessionSsePort {

    /**
     * 创建会话的 SSE 推送通道
     * @param sessionId 会话唯一标识
     * @return SSE 推送 Sink
     */
    Sinks.Many<ServerSentEvent<String>> create(String sessionId);

    /**
     * 获取会话的 SSE 推送通道
     * @param sessionId 会话唯一标识
     * @return SSE 推送 Sink
     */
    Sinks.Many<ServerSentEvent<String>> get(String sessionId);

    /**
     * 移除会话的 SSE 推送通道
     * @param sessionId 会话唯一标识
     */
    void remove(String sessionId);
}