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
        log.info("SessionNode接收会话创建请求，请求参数: {}", request);

        // 核心流程：创建会话 → 构建事件流 → 全局异常处理
        return sessionManagementService
                // 1. 异步创建会话：生成sessionId、初始化Sink等核心会话资源
                .createSession(request)
                // 2. 会话创建成功后，转换为Flux并构建事件流（Mono转Flux适配SSE响应）
                .flatMapMany(session -> {
                    // 2.1 写入会话上下文：供EndNode等后续节点获取会话配置
                    context.setSessionConfigVO(session);
                    String sessionId = session.getSessionId();
                    log.info("会话创建成功，会话ID: {}", sessionId);

                    // 2.2 构建会话初始化事件：必须优先返回给前端，用于前端绑定会话标识
                    ServerSentEvent<String> initEvent = ServerSentEvent
                            .<String>builder()
                            .event("session") // 事件类型：session（前端识别为会话初始化）
                            .data(sessionId)  // 事件数据：唯一会话ID
                            .build();

                    // 2.3 延迟执行责任链路由（关键写法）：
                    // 使用Flux.defer延迟router调用，避免提前触发后续流程（如EndNode的SSE流）
                    // 保证初始化事件优先返回，且router执行异常可被全局onErrorResume捕获
                    Flux<ServerSentEvent<String>> nextFlux = Flux.defer(() -> {
                        try {
                            // 路由至责任链下一个节点（EndNode）
                            return router(request, context);
                        } catch (Exception e) {
                            // 包装异常为运行时异常，保证响应式流能捕获并传播
                            throw new RuntimeException(e);
                        }
                    });

                    // 2.4 顺序拼接事件流：先返回初始化事件，再拼接EndNode的SSE主流
                    // 保证前端先拿到sessionId，再接收后续业务消息/心跳
                    return Flux.concat(Flux.just(initEvent), nextFlux);
                })

                // 3. 全局异常捕获：流内任意环节异常时，返回标准化错误事件
                // 设计意图：避免SSE流中断且无反馈，前端可通过"error"事件感知并处理异常
                .onErrorResume(e -> {
                    log.error("SessionNode处理会话创建请求时发生异常", e);
                    return Flux.just(ServerSentEvent
                            .<String>builder()
                            .event("error")       // 事件类型：error（前端识别为异常）
                            .data(e.getMessage()) // 事件数据：异常信息
                            .build());
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