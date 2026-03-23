package com.c.cases.mcp.core.session.engine.handler;

import com.c.cases.mcp.core.message.strategy.StrategyHandler;
import com.c.cases.mcp.core.session.factory.DefaultMcpSessionFactory;
import com.c.cases.mcp.support.AbstractMcpSessionSupport;
import com.c.domain.session.model.valobj.SessionConfigVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;

/**
 * MCP会话核心入口节点（责任链首节点）
 * 1. 接收前端会话创建请求，调用会话管理服务生成sessionId和消息发送器（Sink）
 * 2. 将会话配置写入上下文，供责任链后续节点共享
 * 3. 向前端推送会话初始化事件（携带sessionId）
 * 4. 路由至EndNode接管SSE长连接，保证事件流顺序与延迟执行的正确性
 *
 * @author cyh
 * @date 2026/03/19
 */
@Slf4j
@Service
public class SessionNode extends AbstractMcpSessionSupport {

    /** 注入责任链末端节点 ：接管SSE流的最终处理（心跳保活、消息推送、会话资源清理） */
    @Resource
    private EndNode endNode;

    /**
     * 责任链核心执行方法（会话创建入口）
     * 重写自AbstractMcpSessionSupport模板方法，封装会话创建全流程
     *
     * @param request 前端传入的会话创建请求参数（如业务标识、初始化参数等）
     * @param context 会话动态上下文，用于责任链节点间共享会话配置
     * @return Flux<ServerSentEvent < String>> 组合后的SSE事件流（初始化事件+业务流/错误事件）
     */
    @Override
    protected Flux<ServerSentEvent<String>> doApply(String request, DefaultMcpSessionFactory.DynamicContext context) {
        return sessionManagementService
                .createSession(request)
                .flatMapMany(session -> {
                    context.setSessionConfigVO(session);
                    String sessionId = session.getSessionId();

                    String endpointPath = String.format("%s/api/v1/gateways/%s/sessions/%s/messages", "/api-gateway",
                            request, sessionId);

                    log.info("向客户端推送标准 MCP endpoint: {}", endpointPath);

                    // 只有发了 event("endpoint")，SDK 才会停止在 SSE 流上瞎解析 JSON，转而去发 POST 请求
                    ServerSentEvent<String> endpointEvent = ServerSentEvent
                            .<String>builder()
                            .event("endpoint")
                            .data(endpointPath)
                            .build();

                    Flux<ServerSentEvent<String>> nextFlux = Flux.defer(() -> {
                        try {
                            return router(request, context);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

                    return Flux.concat(Flux.just(endpointEvent), nextFlux);
                });
    }

    /**
     * 责任链路由方法（策略模式实现）
     * 指定当前节点的下一个处理器为EndNode，实现责任链流转
     *
     * @param request 前端请求参数
     * @param context 会话动态上下文
     * @return EndNode实例（责任链下一个节点，处理SSE流生命周期）
     */
    @Override
    public StrategyHandler<String, DefaultMcpSessionFactory.DynamicContext, Flux<ServerSentEvent<String>>> get(String request, DefaultMcpSessionFactory.DynamicContext context) {
        // 路由至EndNode：由其接管SSE流的心跳保活、消息推送和资源清理
        return endNode;
    }
}