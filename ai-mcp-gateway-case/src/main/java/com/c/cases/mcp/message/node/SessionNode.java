package com.c.cases.mcp.message.node;

import com.c.cases.mcp.api.model.McpSessionRequest;
import com.c.cases.mcp.framework.tree.StrategyHandler;
import com.c.cases.mcp.message.AbstractMcpSessionSupport;
import com.c.cases.mcp.message.factory.DefaultMcpMessageFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * 会话创建节点
 * 负责创建会话、初始化SSE通道、生成并推送endpoint路径
 *
 * @author cyh
 * @date 2026/03/24
 */
@Slf4j
@Service("mcpMessageSessionNode")
public class SessionNode extends AbstractMcpSessionSupport {

    @Resource(name = "mcpMessageEndNode")
    private EndNode endNode;

    /**
     * 会话节点核心逻辑：创建会话、构建SSE通道、推送endpoint
     *
     * @param request 请求参数
     * @param context 动态上下文
     * @return SSE事件流
     */
    @Override
    protected Flux<ServerSentEvent<String>> doApply(String request, DefaultMcpMessageFactory.DynamicContext context) {
        // 从上下文获取会话请求对象
        McpSessionRequest sessionRequest = context.getSessionRequest();
        // 获取超时时间：请求对象为空则使用默认值
        Integer timeoutSeconds = sessionRequest == null ? null : sessionRequest.getTimeout();

        // 创建会话并执行后续逻辑
        return sessionManagementService
                .createSession(request, timeoutSeconds)
                .flatMapMany(session -> {
                    // 将创建好的会话存入上下文
                    context.setSession(session);

                    // 创建SSE推送通道并放入上下文
                    Sinks.Many<ServerSentEvent<String>> sink = sessionSsePort.create(session.getSessionId());
                    context.setSink(sink);

                    // 构造客户端消息接收endpoint路径
                    String endpointPath = String.format("/api-gateway/api/v1/gateways/%s/sessions/%s/messages",
                            request, session.getSessionId());
                    // 将endpoint路径存入上下文
                    context.setEndpointPath(endpointPath);
                    log.info("向客户端推送标准 MCP endpoint: {}", endpointPath);

                    // 构造endpoint事件，用于客户端接收消息路径
                    ServerSentEvent<String> endpointEvent = ServerSentEvent
                            .<String>builder()
                            .event("endpoint")
                            .data(endpointPath)
                            .build();

                    // 延迟路由：等待endpoint推送完成后执行下一个节点
                    Flux<ServerSentEvent<String>> nextFlux = Flux.defer(() -> {
                        try {
                            return router(request, context);
                        } catch (Exception e) {
                            return Flux.error(e);
                        }
                    });

                    // 先推送endpoint，再执行后续节点流
                    return Flux.concat(Flux.just(endpointEvent), nextFlux);
                });
    }

    /**
     * 策略匹配：路由到最终节点
     *
     * @param request 请求参数
     * @param context 动态上下文
     * @return 最终节点处理器
     */
    @Override
    public StrategyHandler<String, DefaultMcpMessageFactory.DynamicContext, Flux<ServerSentEvent<String>>> get(String request, DefaultMcpMessageFactory.DynamicContext context) {
        return endNode;
    }
}