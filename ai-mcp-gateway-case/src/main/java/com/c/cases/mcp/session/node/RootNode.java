package com.c.cases.mcp.session.node;

import com.c.cases.mcp.framework.tree.StrategyHandler;
import com.c.cases.mcp.session.AbstractMcpSessionSupport;
import com.c.cases.mcp.session.factory.DefaultMcpSessionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 责任链根节点
 * 会话创建流程的统一入口，负责路由到校验节点
 *
 * @author cyh
 * @date 2026/03/24
 */
@Slf4j
@Service
public class RootNode extends AbstractMcpSessionSupport {

    @jakarta.annotation.Resource
    private VerifyNode verifyNode;

    /**
     * 根节点入口逻辑：打印日志并路由到下一个节点
     *
     * @param request 请求参数
     * @param context 动态上下文
     * @return SSE事件流
     * @throws Exception 执行异常
     */
    @Override
    protected Flux<ServerSentEvent<String>> doApply(String request, DefaultMcpSessionFactory.DynamicContext context) throws Exception {
        log.info("RootNode 处理请求 | gatewayId={}", request);
        // 路由到下一个执行节点
        return router(request, context);
    }

    /**
     * 策略匹配：路由到校验节点
     *
     * @param request 请求参数
     * @param context 动态上下文
     * @return 校验节点处理器
     */
    @Override
    public StrategyHandler<String, DefaultMcpSessionFactory.DynamicContext, Flux<ServerSentEvent<String>>> get(
            String request, DefaultMcpSessionFactory.DynamicContext context) {
        return verifyNode;
    }
}