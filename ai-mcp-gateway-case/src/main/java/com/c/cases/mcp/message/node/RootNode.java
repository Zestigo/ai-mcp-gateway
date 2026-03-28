package com.c.cases.mcp.message.node;

import com.c.cases.mcp.framework.tree.StrategyHandler;
import com.c.cases.mcp.message.AbstractMcpMessageSupport;
import com.c.cases.mcp.message.factory.DefaultMcpMessageFactory;
import com.c.domain.session.model.entity.HandleMessageCommandEntity;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

/**
 * MCP 消息处理责任链入口根节点
 * 作为消息处理的统一入口，负责参数校验、日志记录与责任链分发
 *
 * @author cyh
 * @date 2026/03/28
 */
@Slf4j
@Service("mcpMessageRootNode")
public class RootNode extends AbstractMcpMessageSupport {

    /** 限流节点，作为流量第一道防护 */
    @Resource(name = "mcpMessageRateLimitNode")
    private RateLimitNode rateLimitNode;

    /**
     * 根节点核心处理逻辑
     * 执行快速失败校验，记录接入日志，转发至限流节点
     *
     * @param request 消息处理命令实体
     * @param context 动态上下文
     * @return 响应结果异步对象
     */
    @Override
    protected Mono<ResponseEntity<Void>> doApply(HandleMessageCommandEntity request,
                                                 DefaultMcpMessageFactory.DynamicContext context) {
        // 1. 基础参数非空校验
        Assert.notNull(request, "HandleMessageCommandEntity cannot be null");
        Assert.hasText(request.getSessionId(), "SessionId is required");

        // 2. 记录链路接入日志
        log.info(">>> [RootNode] MCP 链路接入成功 | sessionId: {} | gatewayId: {}", request.getSessionId(),
                request.getGatewayId());

        // 3. 路由至限流节点，包装异常捕获
        return Mono.defer(() -> {
            try {
                return router(request, context);
            } catch (Exception e) {
                log.error("[RootNode] 路由转发至限流节点异常", e);
                return Mono.error(e);
            }
        });
    }

    /**
     * 指定责任链下一个执行节点
     * 根节点统一转发至限流节点，形成标准流程
     *
     * @param request 消息处理命令实体
     * @param context 动态上下文
     * @return 限流节点策略处理器
     */
    @Override
    public StrategyHandler<HandleMessageCommandEntity, DefaultMcpMessageFactory.DynamicContext,
            Mono<ResponseEntity<Void>>> get(HandleMessageCommandEntity request,
                                            DefaultMcpMessageFactory.DynamicContext context) {
        return rateLimitNode;
    }
}