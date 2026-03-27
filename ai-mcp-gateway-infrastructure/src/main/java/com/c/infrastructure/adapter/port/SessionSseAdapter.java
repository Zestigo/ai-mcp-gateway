package com.c.infrastructure.adapter.port;

import com.c.domain.session.adapter.repository.SessionSsePort;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SessionSsePort 适配器实现
 * 基于内存 ConcurrentHashMap 管理 SSE 连接 Sink
 *
 * @author cyh
 * @date 2026/03/27
 */
@Component
public class SessionSseAdapter implements SessionSsePort {

    /** 会话ID -> SSE 广播通道 */
    private final Map<String, Sinks.Many<ServerSentEvent<String>>> sinks = new ConcurrentHashMap<>();

    /**
     * 创建多路广播 Sink，支持背压缓冲
     *
     * @param sessionId 会话ID
     * @return Sinks.Many
     */
    @Override
    public Sinks.Many<ServerSentEvent<String>> create(String sessionId) {
        return sinks.computeIfAbsent(sessionId, k -> Sinks
                .many()
                .multicast()
                .onBackpressureBuffer());
    }

    /**
     * 获取对应会话的 Sink
     *
     * @param sessionId 会话ID
     * @return Sink
     */
    @Override
    public Sinks.Many<ServerSentEvent<String>> get(String sessionId) {
        return sinks.get(sessionId);
    }

    /**
     * 移除 Sink 并关闭通道
     *
     * @param sessionId 会话ID
     */
    @Override
    public void remove(String sessionId) {
        Sinks.Many<ServerSentEvent<String>> sink = sinks.remove(sessionId);
        if (sink != null) {
            // 发送完成信号，关闭流
            sink.tryEmitComplete();
        }
    }

    /**
     * 向指定会话发送 SSE 事件
     *
     * @param sessionId 会话ID
     * @param event     SSE 事件
     */
    @Override
    public void send(String sessionId, ServerSentEvent<String> event) {
        Sinks.Many<ServerSentEvent<String>> sink = sinks.get(sessionId);
        if (sink != null) {
            // 非阻塞发送，不影响调用线程
            sink.tryEmitNext(event);
        }
    }
}