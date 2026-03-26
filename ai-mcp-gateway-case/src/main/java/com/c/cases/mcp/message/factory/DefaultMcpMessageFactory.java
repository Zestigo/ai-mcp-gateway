package com.c.cases.mcp.message.factory;

import com.c.cases.mcp.api.model.McpSessionRequest;
import com.c.cases.mcp.framework.tree.StrategyHandler;
import com.c.cases.mcp.message.node.RootNode;
import com.c.domain.session.model.entity.McpSession;
import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * 默认 MCP 会话工厂
 * 提供会话创建责任链的入口，对外提供策略处理器
 *
 * @author cyh
 * @date 2026/03/24
 */
@Component
public class DefaultMcpMessageFactory {

    @Resource(name = "mcpMessageRootNode")
    private RootNode rootNode;

    /**
     * 获取会话创建根节点策略处理器
     *
     * @param gatewayId 网关标识
     * @return 根节点处理器
     */
    public StrategyHandler<String, DynamicContext, Flux<ServerSentEvent<String>>> strategyHandler(String gatewayId) {
        return rootNode;
    }

    @Data
    public static class DynamicContext {
        /** 会话请求对象 */
        private McpSessionRequest sessionRequest;

        /** 网关配置对象 */
        private McpGatewayConfigVO gatewayConfigVO;

        /** 会话实体对象 */
        private McpSession session;

        /** SSE消息推送通道 */
        private Sinks.Many<ServerSentEvent<String>> sink;

        /** 消息接收端点路径 */
        private String endpointPath;
    }
}