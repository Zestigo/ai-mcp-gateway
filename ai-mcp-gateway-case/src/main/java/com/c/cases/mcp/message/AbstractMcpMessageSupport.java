package com.c.cases.mcp.message;

import com.c.cases.mcp.framework.tree.AbstractMultiThreadStrategyRouter;
import com.c.cases.mcp.message.factory.DefaultMcpMessageFactory;
import com.c.domain.session.model.entity.HandleMessageCommandEntity;
import com.c.domain.session.service.SessionManagementService;
import com.c.domain.session.service.message.SessionMessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

/**
 * MCP 消息处理节点的抽象支撑类
 * 统一注入业务服务，并定义多线程行为默认规则
 *
 * @author cyh
 * @date 2026/03/27
 */
@Slf4j
public abstract class AbstractMcpMessageSupport extends AbstractMultiThreadStrategyRouter<HandleMessageCommandEntity,
        DefaultMcpMessageFactory.DynamicContext, Mono<ResponseEntity<Void>>> {

    /** 消息业务处理服务 */
    @Resource
    protected SessionMessageService sessionMessageService;

    /** 会话管理服务 */
    @Resource
    protected SessionManagementService sessionManagementService;

    /**
     * 多线程策略配置
     *
     * @param request 消息命令
     * @param context 上下文
     */
    @Override
    protected void multiThread(HandleMessageCommandEntity request, DefaultMcpMessageFactory.DynamicContext context) {
        // 基于 WebFlux 模型，默认不切换线程，保持在 EventLoop 以降低切换开销
    }
}