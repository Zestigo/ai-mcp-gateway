package com.c.cases.mcp.session.factory;

import com.c.cases.mcp.api.model.McpSessionRequest;
import com.c.cases.mcp.framework.tree.StrategyHandler;
import com.c.cases.mcp.session.node.RootNode;
import com.c.domain.session.model.entity.McpSession;
import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import lombok.Data;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import jakarta.annotation.Resource;
import reactor.core.publisher.Sinks;

/**
 * 默认 MCP 会话工厂
 * 提供会话创建责任链的入口，对外提供策略处理器
 *
 * @author cyh
 * @date 2026/03/24
 */
@Component
public class DefaultMcpSessionFactory {

    @Resource(name = "mcpSessionRootNode")
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
        /** 1. 原始输入：会话请求对象 (内含 gatewayId, apiKey) */
        private McpSessionRequest sessionRequest;

        /** 2. 运行时配置：从数据库/配置中心加载的网关规则 */
        private McpGatewayConfigVO gatewayConfigVO;

        /** 3. 产出物：创建成功的会话实体 */
        private McpSession session;

        /** 4. 管道：SSE消息推送通道 */
        private Sinks.Many<ServerSentEvent<String>> sink;

        /** 5. 辅助信息：消息接收端点路径 */
        private String endpointPath;

        /** * 快捷获取 API Key
         * 这样在 Node 节点里直接 context.getApiKey()，代码极简
         */
        public String getApiKey() {
            return sessionRequest != null ? sessionRequest.getApiKey() : null;
        }
    }
}