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
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * MCP会话应用层服务实现
 * 负责SSE长连接生命周期管理：策略链调度 -> 心跳挂载 -> 自动化资源回收
 * 统一处理会话创建、关闭、异常与资源释放逻辑
 *
 * @author cyh
 * @date 2026/03/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpSessionServiceImpl implements McpSessionService {

    /** MCP会话工厂，用于创建策略链执行器 */
    private final DefaultMcpSessionFactory mcpSessionFactory;

    /** 会话管理领域服务，负责会话存储与生命周期控制 */
    private final SessionManagementService sessionManagementService;

    /**
     * 简易入口：适配 Controller 层的直接调用
     * 封装基础参数构建请求对象，调用完整创建逻辑
     *
     * @param gatewayId 网关唯一标识
     * @param apiKey    接口访问密钥
     * @return SSE事件流，用于前端长连接通信
     */
    @Override
    public Flux<ServerSentEvent<String>> createMcpSession(String gatewayId, String apiKey) {
        return createMcpSession(McpSessionRequest
                .builder()
                .gatewayId(gatewayId)
                .apiKey(apiKey)
                .build());
    }

    /**
     * 完整入口：执行策略树并维护长连接生命周期
     * 包含参数校验、策略执行、心跳合并、资源回收、异常处理全流程
     *
     * @param request 会话创建请求参数
     * @return SSE事件流，包含业务消息与心跳消息
     */
    @Override
    public Flux<ServerSentEvent<String>> createMcpSession(McpSessionRequest request) {
        // 1. 基础校验：快速失败，空参数直接拦截
        Assert.notNull(request, "McpSessionRequest cannot be null");
        String gatewayId = request.getGatewayId();
        // 敏感信息脱敏处理，避免日志泄露
        String maskedKey = request.getApiKey() != null ? request
                .getApiKey()
                .substring(0, Math.min(request
                        .getApiKey()
                        .length(), 6)) + "***" : "NULL";

        // 2. 初始化上下文：统一管理请求数据与中间状态
        DefaultMcpSessionFactory.DynamicContext context = new DefaultMcpSessionFactory.DynamicContext();
        context.setSessionRequest(request);

        // 3. 获取策略链执行器，根据网关ID路由对应处理逻辑
        StrategyHandler<String, DefaultMcpSessionFactory.DynamicContext, Flux<ServerSentEvent<String>>> strategyHandler = mcpSessionFactory.strategyHandler(gatewayId);

        return Flux.defer(() -> {
                       try {
                           log.info("[会话准备] 开始执行策略链 | gatewayId: {}, apiKey: {}", gatewayId, maskedKey);
                           // 执行策略链，回填会话配置与对象到上下文
                           return strategyHandler.apply(gatewayId, context);
                       } catch (Exception e) {
                           log.error("[策略异常] 策略链执行中断 | gatewayId: {}", gatewayId, e);
                           return Flux.error(e);
                       }
                   })
                   // 4. 合并心跳流，与业务流并行发送维持长连接
                   .mergeWith(heartbeat())
                   // 订阅时记录连接建立日志
                   .doOnSubscribe(s -> log.info("[SSE激活] 实时消息管道已建立 | gatewayId: {}", gatewayId))

                   // 5. 资源自动清理：覆盖正常结束、取消、异常所有场景
                   .doFinally(signalType -> {
                       if (context.getSession() != null) {
                           String sessionId = context
                                   .getSession()
                                   .getSessionId();
                           log.info("[资源回收] 收到信号: {}, 开始清理会话 | sessionId: {}", signalType, sessionId);

                           // 异步清理，防止阻塞Netty IO线程
                           sessionManagementService
                                   .removeSession(sessionId)
                                   .subscribeOn(Schedulers.boundedElastic())
                                   .doOnSuccess(v -> log.debug("[资源回收] 会话清理完成 | sessionId: {}", sessionId))
                                   .subscribe();
                       }
                   })

                   // 6. 全局异常兜底，保证前端可感知错误
                   .onErrorResume(e -> {
                       log.error("[SSE链路异常] 运行中触发错误 | gatewayId: {}, 错误类型: {}", gatewayId, e
                               .getClass()
                               .getSimpleName());
                       return Flux.just(ServerSentEvent
                               .<String>builder()
                               .event("error")
                               .data("Session encounter internal error: " + e.getMessage())
                               .build());
                   });
    }

    /**
     * 显式关闭会话
     * 主动触发会话资源清理，支持外部手动关闭连接
     *
     * @param sessionId 会话唯一标识
     * @return 异步关闭完成信号
     */
    @Override
    public Mono<Void> closeSession(String sessionId) {
        log.info("[手动下线] 显式关闭请求 | sessionId: {}", sessionId);
        return sessionManagementService.removeSession(sessionId);
    }

    /**
     * 标准 SSE 心跳生成器
     * 每30秒发送注释型心跳，不干扰业务消息且维持连接存活
     *
     * @return 周期性心跳事件流
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