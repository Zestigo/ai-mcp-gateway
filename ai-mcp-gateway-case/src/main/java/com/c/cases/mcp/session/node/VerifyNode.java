package com.c.cases.mcp.session.node;

import com.c.cases.mcp.framework.tree.StrategyHandler;
import com.c.cases.mcp.session.AbstractMcpSessionSupport;
import com.c.cases.mcp.session.factory.DefaultMcpSessionFactory;
import com.c.types.exception.AppException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 网关校验节点
 * 负责校验网关配置是否存在，合法性校验
 *
 * @author cyh
 * @date 2026/03/24
 */
@Slf4j
@Service
public class VerifyNode extends AbstractMcpSessionSupport {

    @Resource
    private SessionNode sessionNode;

    /**
     * 校验节点核心逻辑：校验网关配置是否存在
     *
     * @param request 请求参数
     * @param context 动态上下文
     * @return SSE事件流
     * @throws Exception 校验异常
     */
    @Override
    protected Flux<ServerSentEvent<String>> doApply(String request, DefaultMcpSessionFactory.DynamicContext context) throws Exception {
        log.info("VerifyNode 校验 | gatewayId={}", request);

        // 查询网关配置信息
        var config = sessionRepository.queryMcpGatewayConfigByGatewayId(request);
        // 配置不存在则抛出业务异常
        if (config == null) {
            throw new AppException("0003", "未找到网关配置: " + request);
        }

        // 将网关配置存入上下文
        context.setGatewayConfigVO(config);
        // 路由到下一个节点
        return router(request, context);
    }

    /**
     * 策略匹配：路由到会话创建节点
     *
     * @param request 请求参数
     * @param context 动态上下文
     * @return 会话创建节点处理器
     */
    @Override
    public StrategyHandler<String, DefaultMcpSessionFactory.DynamicContext, Flux<ServerSentEvent<String>>> get(
            String request, DefaultMcpSessionFactory.DynamicContext context) {
        return sessionNode;
    }
}