package com.c.cases.mcp.core.session.engine.handler;

import com.c.cases.mcp.core.session.factory.DefaultMcpSessionFactory;
import com.c.cases.mcp.core.message.strategy.StrategyHandler;
import com.c.cases.mcp.support.AbstractMcpSessionSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import jakarta.annotation.Resource;

/**
 * MCP会话责任链根节点：作为责任链入口，路由至验证节点
 *
 * @author cyh
 * @date 2026/03/19
 */
@Slf4j
@Service
public class RootNode extends AbstractMcpSessionSupport {

    /** 验证节点：责任链下一个执行节点 */
    @Resource
    private VerifyNode verifyNode;

    /**
     * 执行根节点逻辑：记录日志并路由至下一个节点
     *
     * @param request 请求参数（网关ID）
     * @param context 会话动态上下文
     * @return 下一个节点的SSE事件流
     * @throws Exception 路由过程中抛出的异常
     */
    @Override
    protected Flux<ServerSentEvent<String>> doApply(String request, DefaultMcpSessionFactory.DynamicContext context) throws Exception {
        log.info("RootNode: {}", request);
        return router(request, context);
    }

    /**
     * 获取下一个策略处理器：返回验证节点作为责任链下一环
     *
     * @param request 请求参数
     * @param context 会话动态上下文
     * @return 验证节点实例
     */
    @Override
    public StrategyHandler<String, DefaultMcpSessionFactory.DynamicContext, Flux<ServerSentEvent<String>>> get(String request, DefaultMcpSessionFactory.DynamicContext context) {
        return verifyNode;
    }
}