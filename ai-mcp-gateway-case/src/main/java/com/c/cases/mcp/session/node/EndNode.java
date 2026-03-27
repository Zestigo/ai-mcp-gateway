package com.c.cases.mcp.session.node;

import com.c.cases.mcp.framework.tree.StrategyHandler;
import com.c.cases.mcp.session.AbstractMcpSessionSupport;
import com.c.cases.mcp.session.factory.DefaultMcpSessionFactory;
import com.c.domain.session.model.entity.McpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * SSE 长连接最终处理节点
 * 负责输出业务流、心跳保活、资源自动释放
 *
 * @author cyh
 * @date 2026/03/27
 */
@Slf4j
@Service("mcpSessionEndNode")
public class EndNode extends AbstractMcpSessionSupport {

    /**
     * 构建 SSE 输出流，包含业务消息 + 心跳，并管理生命周期
     *
     * @param requestParameter 请求参数
     * @param context          动态上下文
     * @return SSE 事件流
     */
    @Override
    protected Flux<ServerSentEvent<String>> doApply(String requestParameter,
                                                    DefaultMcpSessionFactory.DynamicContext context) {
        McpSession session = context.getSession();
        // 校验会话与 Sink 完整性
        if (session == null || context.getSink() == null) {
            return Flux.error(new IllegalStateException("Session context is incomplete"));
        }

        String sessionId = session.getSessionId();
        log.info("EndNode 已接管 SSE 长连接流 | sessionId: {}", sessionId);

        // 1. 构建心跳流：每 30s 发送一条注释 ping，维持长连接不断开
        Flux<ServerSentEvent<String>> heartbeatFlux = Flux
                .interval(Duration.ofSeconds(30))
                .map(i -> ServerSentEvent
                        .<String>builder()
                        .comment("ping")
                        .build())
                .doOnNext(e -> log.debug("发送心跳包 | sessionId: {}", sessionId));

        // 2. 业务消息流与心跳流合并，作为最终输出流
        Flux<ServerSentEvent<String>> businessFlux = context
                .getSink()
                .asFlux()
                .mergeWith(heartbeatFlux);

        // 3. usingWhen 确保无论正常结束、异常、取消，都会执行资源释放
        return Flux.usingWhen(Mono.just(session), s -> businessFlux,
                // 正常完成时释放
                s -> release(sessionId, "NORMAL_COMPLETION"),
                // 异常时释放
                (s, e) -> {
                    log.error("SSE 流发生异常 | sessionId: {} | error: {}", sessionId, e.getMessage());
                    return release(sessionId, "ERROR");
                },
                // 客户端断开（cancel）时释放
                s -> {
                    log.info("客户端主动断开连接 (Cancel) | sessionId: {}", sessionId);
                    return release(sessionId, "CLIENT_DISCONNECT");
                });
    }

    /**
     * 统一释放会话资源：删除会话记录、关闭 SSE 通道
     *
     * @param sessionId 会话ID
     * @param reason    释放原因
     * @return 释放完成 Mono
     */
    private Mono<Void> release(String sessionId, String reason) {
        return Mono.defer(() -> {
            log.info("开始释放会话资源 | 原因: {} | sessionId: {}", reason, sessionId);
            return sessionManagementService
                    .removeSession(sessionId)
                    .then(Mono.fromRunnable(() -> sessionSsePort.remove(sessionId)))
                    .doOnSuccess(v -> log.info("会话资源释放成功 | sessionId: {}", sessionId))
                    .doOnError(e -> log.error("会话资源释放失败 | sessionId: {}", sessionId, e))
                    .then();
        });
    }

    /**
     * 终止责任链，返回默认处理器
     */
    @Override
    public StrategyHandler<String, DefaultMcpSessionFactory.DynamicContext, Flux<ServerSentEvent<String>>> get(String requestParameter, DefaultMcpSessionFactory.DynamicContext context) {
        return defaultStrategyHandler;
    }
}