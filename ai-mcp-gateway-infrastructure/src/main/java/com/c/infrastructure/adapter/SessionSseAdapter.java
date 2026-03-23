package com.c.infrastructure.adapter;

import com.c.domain.session.adapter.repository.SessionSsePort;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话SSE连接存储适配器
 *
 * @author cyh
 * @date 2026/03/24
 */
@Component
public class SessionSseAdapter implements SessionSsePort {

    /** SSE推送通道 */
    private final Map<String, Sinks.Many<ServerSentEvent<String>>> sinks = new ConcurrentHashMap<>();

    /**
     * 创建SSE通道
     *
     * @param sessionId 会话标识
     * @return 推送通道
     */
    @Override
    public Sinks.Many<ServerSentEvent<String>> create(String sessionId) {
        // 多播通道，支持背压
        return sinks.computeIfAbsent(sessionId, k -> Sinks
                .many()
                .multicast()
                .onBackpressureBuffer());
    }

    /**
     * 获取SSE通道
     *
     * @param sessionId 会话标识
     * @return 推送通道
     */
    @Override
    public Sinks.Many<ServerSentEvent<String>> get(String sessionId) {
        return sinks.get(sessionId);
    }

    /**
     * 移除SSE通道
     *
     * @param sessionId 会话标识
     */
    @Override
    public void remove(String sessionId) {
        Sinks.Many<ServerSentEvent<String>> sink = sinks.remove(sessionId);
        if (sink != null) {
            // 关闭通道
            sink.tryEmitComplete();
        }
    }
}