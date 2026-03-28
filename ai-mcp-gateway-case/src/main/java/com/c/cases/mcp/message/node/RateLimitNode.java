package com.c.cases.mcp.message.node;

import com.c.cases.mcp.framework.tree.StrategyHandler;
import com.c.cases.mcp.message.AbstractMcpMessageSupport;
import com.c.cases.mcp.message.factory.DefaultMcpMessageFactory;
import com.c.domain.auth.model.entity.RateLimitCommandEntity;
import com.c.domain.session.model.entity.HandleMessageCommandEntity;
import com.c.domain.auth.service.AuthRateLimitService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * MCP 消息限流节点
 * 基于API Key与网关ID实现流量控制，防止系统过载
 * 是消息处理链路的第一道安全防护
 *
 * @author cyh
 * @date 2026/03/28
 */
@Slf4j
@Service("mcpMessageRateLimitNode")
public class RateLimitNode extends AbstractMcpMessageSupport {

    /** 会话校验节点，限流通过后流转至此 */
    @Resource(name = "mcpMessageSessionNode")
    private SessionNode sessionNode;

    /** 认证限流领域服务，基于Redis实现分布式限流 */
    @Resource
    private AuthRateLimitService authRateLimitService;

    /**
     * 执行限流校验逻辑
     * 构造限流命令，调用限流服务，根据结果放行或拦截
     *
     * @param request 消息处理命令实体
     * @param context 动态上下文
     * @return 响应结果异步对象，限流时返回429
     */
    @Override
    protected Mono<ResponseEntity<Void>> doApply(HandleMessageCommandEntity request,
                                                 DefaultMcpMessageFactory.DynamicContext context) {

        // 构造限流校验参数实体
        RateLimitCommandEntity command = RateLimitCommandEntity
                .builder()
                .gatewayId(request.getGatewayId())
                .apiKey(request.getApiKey())
                .build();

        // 执行限流校验：true=限流，false=放行
        return authRateLimitService
                .rateLimit(command)
                .flatMap(isLimited -> {
                    if (Boolean.TRUE.equals(isLimited)) {
                        // 触发限流，返回429状态码
                        log.warn("[限流拦截] 触发策略拦截 | sessionId: {}", request.getSessionId());
                        return Mono.just(ResponseEntity
                                .status(HttpStatus.TOO_MANY_REQUESTS)
                                .<Void>build());
                    } else {
                        // 校验通过，路由至下一级节点
                        try {
                            return router(request, context);
                        } catch (Exception e) {
                            return Mono.error(e);
                        }
                    }
                })
                // 限流服务异常时容错放行，保证服务可用性
                .onErrorResume(e -> {
                    log.error("[限流异常] 容错放行", e);
                    try {
                        return router(request, context);
                    } catch (Exception ex) {
                        return Mono.error(ex);
                    }
                });
    }

    /**
     * 获取责任链下一个执行节点
     * 限流通过后路由至会话校验节点
     *
     * @param request 消息处理命令实体
     * @param context 动态上下文
     * @return 会话校验节点策略处理器
     */
    @Override
    public StrategyHandler<HandleMessageCommandEntity, DefaultMcpMessageFactory.DynamicContext,
            Mono<ResponseEntity<Void>>> get(HandleMessageCommandEntity request,
                                            DefaultMcpMessageFactory.DynamicContext context) {
        return sessionNode;
    }
}