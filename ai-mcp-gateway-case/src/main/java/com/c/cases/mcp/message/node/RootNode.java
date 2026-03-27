package com.c.cases.mcp.message.node;

import com.c.cases.mcp.framework.tree.StrategyHandler;
import com.c.cases.mcp.message.AbstractMcpMessageSupport;
import com.c.cases.mcp.message.factory.DefaultMcpMessageFactory;
import com.c.domain.session.model.entity.HandleMessageCommandEntity;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * MCP 消息处理责任链的入口根节点
 * 负责打印接入日志并路由到会话校验节点
 *
 * @author cyh
 * @date 2026/03/27
 */
@Slf4j
@Service("mcpMessageRootNode")
public class RootNode extends AbstractMcpMessageSupport {

    /** 会话校验与加载节点 */
    @Resource(name = "mcpMessageSessionNode")
    private SessionNode sessionNode;

    /**
     * 根节点处理逻辑：日志打印 + 路由转发
     *
     * @param request 消息处理命令
     * @param context 动态上下文
     * @return 处理结果响应 Mono
     * @throws Exception 可能抛出路由执行异常
     */
    @Override
    protected Mono<ResponseEntity<Void>> doApply(HandleMessageCommandEntity request,
                                                 DefaultMcpMessageFactory.DynamicContext context) throws Exception {
        // 记录消息链路接入日志，便于全链路追踪
        log.info("MCP 消息链路接入 | sessionId: {}", request.getSessionId());
        // 路由到下一级节点：会话节点
        return router(request, context);
    }

    /**
     * 指定责任链下一个节点
     *
     * @param request 消息命令
     * @param context 上下文
     * @return 会话节点处理器
     */
    @Override
    public StrategyHandler<HandleMessageCommandEntity, DefaultMcpMessageFactory.DynamicContext,
            Mono<ResponseEntity<Void>>> get(HandleMessageCommandEntity request,
                                            DefaultMcpMessageFactory.DynamicContext context) {
        // 根节点转发至会话节点
        return sessionNode;
    }
}