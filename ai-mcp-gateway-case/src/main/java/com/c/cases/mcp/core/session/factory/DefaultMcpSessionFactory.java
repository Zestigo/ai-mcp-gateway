package com.c.cases.mcp.core.session.factory;

import com.c.cases.mcp.core.session.engine.handler.RootNode;
import com.c.cases.mcp.core.message.strategy.StrategyHandler;
import lombok.Data;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;

/**
 * MCP会话默认工厂：生产策略处理器（返回责任链根节点）
 *
 * @author cyh
 * @date 2026/03/19
 */
@Component
public class DefaultMcpSessionFactory implements IMcpSessionFactory {

    /** 责任链根节点：策略处理器入口 */
    @Resource
    private RootNode rootNode;

    /**
     * 获取策略处理器：返回责任链根节点
     *
     * @return 根节点策略处理器
     */
    @Override
    public StrategyHandler<String, DynamicContext, Flux<ServerSentEvent<String>>> strategyHandler(String gatewayId) {
        return rootNode;
    }

    /**
     * 会话动态上下文：跨节点传递会话数据
     */
    @Data
    public static class DynamicContext {

        private Object sessionConfigVO;

        /**
         * 泛型获取会话配置对象（避免强制类型转换）
         *
         * @param <T> 会话配置对象类型
         * @return 会话配置对象
         */
        @SuppressWarnings("unchecked")
        public <T> T getSessionConfigVO() {
            return (T) sessionConfigVO;
        }
    }
}