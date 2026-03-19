package com.c.cases.mcp.core.message;

import com.c.cases.mcp.api.service.IMcpSessionService;
import com.c.cases.mcp.api.model.McpSessionRequest;
import com.c.cases.mcp.core.session.factory.DefaultMcpSessionFactory;
import com.c.cases.mcp.core.session.factory.IMcpSessionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;

/**
 * MCP会话服务实现类：提供MCP会话创建能力，返回SSE事件流
 *
 * @author cyh
 * @date 2026/03/19
 */
@Slf4j
@Service
public class McpMessageService implements IMcpSessionService {

    /** MCP会话工厂接口：通过网关ID获取对应策略处理器 */
    @Resource
    private IMcpSessionFactory mcpSessionFactory;

    /**
     * 创建MCP会话：通过会话工厂获取策略处理器，执行会话创建逻辑并返回SSE事件流
     *
     * @param gatewayId 网关ID（用于匹配对应策略处理器）
     * @return SSE事件流（异常时返回包含异常信息的错误Flux）
     */
    @Override
    public Flux<ServerSentEvent<String>> createMcpSession(String gatewayId) {
        // Flux.defer：延迟执行，保证每次请求都创建全新的策略执行流程，避免线程安全问题
        return Flux.defer(() -> {
            try {
                // 1. 通过网关ID从工厂获取专属策略处理器 2. 执行策略并传入上下文，启动会话创建流程
                return mcpSessionFactory.strategyHandler(gatewayId)  // 按网关ID匹配差异化策略
                                        .apply(gatewayId, new DefaultMcpSessionFactory.DynamicContext()); //
                // 上下文用于跨节点传递会话数据
            } catch (Exception e) {
                // 异常日志补充网关ID，便于定位具体网关的会话创建问题
                log.error("创建MCP会话失败, gatewayId:{}", gatewayId, e);
                // 响应式规范：异常转为Flux.error返回，不直接抛出，保证流的完整性
                return Flux.error(e);
            }
        });
    }

    /**
     * 创建MCP会话（重载方法）：接收完整请求参数，暂默认返回空Flux（预留扩展）
     *
     * @param request 会话请求对象（包含网关ID、客户端类型等参数）
     * @return 空SSE事件流
     */
    @Override
    public Flux<ServerSentEvent<String>> createMcpSession(McpSessionRequest request) {
        // 预留扩展点：后续可解析request中的clientType/timeout等参数，实现差异化会话创建
        return Flux.empty();
    }
}