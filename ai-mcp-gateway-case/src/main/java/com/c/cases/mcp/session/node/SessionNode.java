package com.c.cases.mcp.session.node;

import com.c.cases.mcp.api.model.McpSessionRequest;
import com.c.cases.mcp.framework.tree.StrategyHandler;
import com.c.cases.mcp.session.AbstractMcpSessionSupport;
import com.c.cases.mcp.session.factory.DefaultMcpSessionFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 会话创建策略节点
 * 负责创建MCP会话、注册SSE连接、生成并推送endpoint事件
 * 作为策略链入口，完成后路由至EndNode
 *
 * @author cyh
 * @date 2026/03/27
 */
@Slf4j
@Service("mcpSessionSessionNode")
public class SessionNode extends AbstractMcpSessionSupport {

    /** 策略链下一级执行节点 */
    @Resource(name = "mcpSessionEndNode")
    private EndNode endNode;

    /** 服务对外暴露基础地址 */
    @Value("${service.full-url:http://localhost:8080}")
    private String serviceFullUrl;

    /** 应用上下文路径 */
    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * 执行会话创建核心逻辑
     *
     * @param gatewayId 网关唯一标识
     * @param context   动态上下文
     * @return SSE事件流
     */
    @Override
    protected Flux<ServerSentEvent<String>> doApply(String gatewayId, DefaultMcpSessionFactory.DynamicContext context) {
        log.info("[策略节点] SessionNode 开始初始化会话 | gatewayId: {}", gatewayId);

        // 从上下文获取请求参数，提取超时时间
        McpSessionRequest sessionRequest = context.getSessionRequest();
        Integer timeoutSeconds = (sessionRequest != null) ? sessionRequest.getTimeout() : null;

        // 调用领域服务创建会话
        return sessionManagementService
                .createSession(gatewayId, timeoutSeconds)
                .flatMapMany(session -> {
                    String sessionId = session.getSessionId();
                    // 将会话存入上下文，供后续节点使用
                    context.setSession(session);

                    // 从本地缓存获取已创建的SSE Sink，存入上下文
                    sessionManagementService
                            .getLocalSink(sessionId)
                            .ifPresent(context::setSink);

                    // 构造客户端后续消息推送的endpoint地址
                    String endpointUrl = String.format("%s%s/api/v1/gateways/%s/sessions/%s/messages", serviceFullUrl
                            , contextPath, gatewayId, sessionId);
                    context.setEndpointPath(endpointUrl);
                    log.info("[会话初始化] 生成消息接收端点 | sessionId: {}, url: {}", sessionId, endpointUrl);

                    // 构造并发送endpoint事件，客户端依赖该地址进行消息上报
                    ServerSentEvent<String> endpointEvent = ServerSentEvent
                            .<String>builder()
                            .event("endpoint")
                            .data(endpointUrl)
                            .build();

                    // 延迟路由：确保endpoint事件发送完成后再进入下一级节点
                    Flux<ServerSentEvent<String>> nextNodeFlux = Flux.defer(() -> {
                        try {
                            return router(gatewayId, context);
                        } catch (Exception e) {
                            log.error("[策略路由] 路由至下一级节点失败 | sessionId: {}", sessionId, e);
                            return Flux.error(e);
                        }
                    });

                    // 先发送endpoint事件，再拼接后续节点流
                    return Flux.concat(Flux.just(endpointEvent), nextNodeFlux);
                })
                .doOnError(e -> log.error("[会话节点异常] 会话初始化处理失败 | gatewayId: {}", gatewayId, e));
    }

    /**
     * 指定策略链下一级处理器
     */
    @Override
    public StrategyHandler<String, DefaultMcpSessionFactory.DynamicContext, Flux<ServerSentEvent<String>>> get(String gatewayId, DefaultMcpSessionFactory.DynamicContext context) {
        return endNode;
    }
}