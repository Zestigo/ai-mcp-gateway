package com.c.cases.mcp.message.node;

import com.c.cases.mcp.framework.tree.StrategyHandler;
import com.c.cases.mcp.message.AbstractMcpSessionSupport;
import com.c.cases.mcp.message.factory.DefaultMcpMessageFactory;
import com.c.domain.session.model.entity.McpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 责任链最终节点
 * 负责维持SSE长连接、心跳保活、连接关闭后的资源释放
 *
 * @author cyh
 * @date 2026/03/24
 */
@Slf4j
@Service("mcpMessageEndNode")
public class EndNode extends AbstractMcpSessionSupport {

    /**
     * 最终节点业务逻辑：接管SSE流，保持长连接并返回心跳
     *
     * @param requestParameter 请求参数
     * @param context          动态上下文
     * @return SSE事件流
     */
    @Override
    protected Flux<ServerSentEvent<String>> doApply(String requestParameter,
                                                    DefaultMcpMessageFactory.DynamicContext context) {
        // 从上下文获取会话对象
        McpSession session = context.getSession();
        // 校验会话与SSE通道完整性
        if (session == null || context.getSink() == null) {
            return Flux.error(new IllegalStateException("Session context is incomplete"));
        }

        // 获取会话ID用于日志打印
        String sessionId = session.getSessionId();
        log.info("EndNode 接管 SSE 流 | sessionId={}", sessionId);

        // 使用usingWhen管理资源生命周期：正常/异常/完成均执行释放逻辑
        return Flux.usingWhen(
                // 资源供给：会话对象
                Mono.just(session),
                // 资源使用：拼接SSE业务流 + 30秒心跳流
                s -> context
                        .getSink()
                        .asFlux()
                        .mergeWith(Flux
                                .interval(Duration.ofSeconds(30))
                                .map(i -> ServerSentEvent
                                        .<String>builder()
                                        .comment("ping")
                                        .build())),
                // 正常完成时释放资源
                s -> release(sessionId),
                // 异常时释放资源
                (s, e) -> release(sessionId),
                // 取消时释放资源
                s -> release(sessionId)
        );
    }

    /**
     * 释放会话资源：删除会话记录并关闭SSE通道
     *
     * @param sessionId 会话ID
     * @return 释放结果Mono
     */
    private Mono<Void> release(String sessionId) {
        // 从仓储删除会话，然后关闭SSE通道
        return sessionManagementService
                .removeSession(sessionId)
                .then(Mono.fromRunnable(() -> sessionSsePort.remove(sessionId)));
    }

    /**
     * 策略匹配：当前为最终节点，无后续节点
     *
     * @param requestParameter 请求参数
     * @param context          动态上下文
     * @return 默认兜底处理器
     */
    @Override
    public StrategyHandler<String, DefaultMcpMessageFactory.DynamicContext, Flux<ServerSentEvent<String>>> get(
            String requestParameter, DefaultMcpMessageFactory.DynamicContext context) {
        return defaultStrategyHandler;
    }
}