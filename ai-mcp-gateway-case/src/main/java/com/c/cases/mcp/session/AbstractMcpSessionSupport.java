package com.c.cases.mcp.session;

import com.c.cases.mcp.framework.tree.AbstractMultiThreadStrategyRouter;
import com.c.cases.mcp.session.factory.DefaultMcpSessionFactory;
import com.c.domain.session.adapter.repository.McpGatewayConfigRepository;
import com.c.domain.session.adapter.repository.SessionSsePort;
import com.c.domain.session.service.ISessionManagementService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * MCP会话支撑抽象类
 * 集成策略路由，注入会话核心依赖，作为所有会话节点的父类
 *
 * @author cyh
 * @date 2026/03/24
 */
@Slf4j
public abstract class AbstractMcpSessionSupport extends AbstractMultiThreadStrategyRouter<String,
        DefaultMcpSessionFactory.DynamicContext, Flux<ServerSentEvent<String>>> {

    /** 会话管理服务 */
    @Resource
    protected ISessionManagementService sessionManagementService;

    /** SSE通道存储接口 */
    @Resource
    protected SessionSsePort sessionSsePort;

    /** 网关配置仓储接口 */
    @Resource
    protected McpGatewayConfigRepository sessionRepository;

    /**
     * 会话流程默认不启用多线程并发
     *
     * @param request 请求参数
     * @param context 动态上下文
     * @throws Exception 扩展异常
     */
    @Override
    protected void multiThread(String request, DefaultMcpSessionFactory.DynamicContext context) throws Exception {
        // 会话创建链路为同步流程，无需并发处理
    }
}