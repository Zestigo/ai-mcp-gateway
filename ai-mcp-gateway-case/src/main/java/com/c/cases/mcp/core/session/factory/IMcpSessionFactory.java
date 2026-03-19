package com.c.cases.mcp.core.session.factory;

import com.c.cases.mcp.core.message.strategy.StrategyHandler;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * MCP会话工厂接口：定义按网关ID获取策略处理器的标准
 *
 * @author cyh
 * @date 2026/03/19
 */
public interface IMcpSessionFactory {

    /**
     * 按网关ID获取专属策略处理器
     *
     * @param gatewayId 网关唯一标识（用于匹配不同网关的差异化会话策略）
     * @return 适配该网关的策略处理器
     * 泛型说明：
     * - String：请求参数类型（网关ID）
     * - DefaultMcpSessionFactory.DynamicContext：会话动态上下文（跨节点传递数据）
     * - Flux<ServerSentEvent<String>>：返回SSE事件流（服务端向客户端推送消息）
     */
    StrategyHandler<String, DefaultMcpSessionFactory.DynamicContext, Flux<ServerSentEvent<String>>> strategyHandler(String gatewayId);
}