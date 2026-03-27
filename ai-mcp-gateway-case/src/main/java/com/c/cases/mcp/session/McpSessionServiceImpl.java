package com.c.cases.mcp.session;

import com.c.cases.mcp.api.model.McpSessionRequest;
import com.c.cases.mcp.api.service.McpSessionService;
import com.c.cases.mcp.framework.tree.StrategyHandler;
import com.c.cases.mcp.session.factory.DefaultMcpSessionFactory;
import com.c.domain.session.service.SessionManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * MCP会话应用层服务实现
 * 负责SSE长连接管理、心跳维护、策略树调度、连接断开自动资源清理
 *
 * @author cyh
 * @date 2026/03/27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpSessionServiceImpl implements McpSessionService {

    /** 会话策略工厂，获取责任链执行器 */
    private final DefaultMcpSessionFactory mcpSessionFactory;

    /** 会话领域服务 */
    private final SessionManagementService sessionManagementService;

    /**
     * 创建MCP会话（简易模式）
     *
     * @param gatewayId 网关唯一标识
     * @return SSE事件流
     */
    @Override
    public Flux<ServerSentEvent<String>> createMcpSession(String gatewayId) {
        return createMcpSession(McpSessionRequest
                .builder()
                .gatewayId(gatewayId)
                .build());
    }

    /**
     * 创建MCP会话（完整模式）
     * 执行策略链、挂载心跳、注册断开自动清理逻辑
     *
     * @param request 会话创建请求
     * @return SSE事件流
     */
    @Override
    public Flux<ServerSentEvent<String>> createMcpSession(McpSessionRequest request) {
        String gatewayId = request.getGatewayId();
        DefaultMcpSessionFactory.DynamicContext context = new DefaultMcpSessionFactory.DynamicContext();
        context.setSessionRequest(request);

        // 获取策略链执行器
        StrategyHandler<String, DefaultMcpSessionFactory.DynamicContext, Flux<ServerSentEvent<String>>> strategyHandler = mcpSessionFactory.strategyHandler(gatewayId);

        return Flux.defer(() -> {
                       try {
                           // 执行会话创建责任链
                           return strategyHandler.apply(gatewayId, context);
                       } catch (Exception e) {
                           log.error("[策略执行] 会话策略链执行失败 | gatewayId: {}", gatewayId, e);
                           return Flux.error(e);
                       }
                   })
                   // 合并心跳流，保持长连接存活
                   .mergeWith(heartbeat())
                   .doOnSubscribe(s -> log.info("[SSE连接建立] 管道已激活 | gatewayId: {}", gatewayId))
                   // 连接断开/异常/完成时，自动清理会话资源
                   .doFinally(signalType -> {
                       if (context.getSession() != null) {
                           String sessionId = context
                                   .getSession()
                                   .getSessionId();
                           log.info("[SSE连接释放] 信号: {}, 开始资源清理 | sessionId: {}", signalType, sessionId);

                           // 异步执行会话销毁，不阻塞主线程
                           sessionManagementService
                                   .removeSession(sessionId)
                                   .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                                   .subscribe();
                       }
                   })
                   .onErrorResume(e -> {
                       log.error("[SSE链路异常] 会话连接运行异常 | gatewayId: {}", gatewayId, e.getMessage());
                       return Flux.empty();
                   });
    }

    /**
     * 显式关闭会话并释放资源
     *
     * @param sessionId 会话唯一标识
     * @return 关闭完成异步信号
     */
    @Override
    public Mono<Void> closeSession(String sessionId) {
        log.info("[显式关闭会话] sessionId: {}", sessionId);
        return sessionManagementService.removeSession(sessionId);
    }

    /**
     * SSE标准心跳流，每30秒发送一次心跳注释
     *
     * @return 心跳事件流
     */
    private Flux<ServerSentEvent<String>> heartbeat() {
        return Flux
                .interval(Duration.ofSeconds(30))
                .map(i -> ServerSentEvent
                        .<String>builder()
                        .comment("ping")
                        .build());
    }
}