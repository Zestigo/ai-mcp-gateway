package com.c.cases.mcp.message.factory;

import com.c.cases.mcp.framework.tree.StrategyHandler;
import com.c.cases.mcp.message.node.RootNode;
import com.c.domain.session.model.entity.HandleMessageCommandEntity;
import com.c.domain.session.model.valobj.SessionConfigVO;
import jakarta.annotation.Resource;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * MCP 消息处理流程的默认工厂类
 * 统一提供消息责任链的入口处理器，并封装动态上下文对象
 *
 * @author cyh
 * @date 2026/03/27
 */
@Component
public class DefaultMcpMessageFactory {

    /** MCP 消息处理责任链的根节点 */
    @Resource(name = "mcpMessageRootNode")
    private RootNode rootNode;

    /**
     * 获取消息处理的顶层策略处理器
     *
     * @return 策略处理器，用于启动整个责任链调用
     */
    public StrategyHandler<HandleMessageCommandEntity, DynamicContext, Mono<ResponseEntity<Void>>> strategyHandler() {
        // 直接返回注入的根节点，作为责任链入口
        return rootNode;
    }

    /**
     * 消息处理全链路共享的动态上下文
     * 用于在责任链各节点之间传递会话配置等运行时数据
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {
        /** 会话相关配置与运行时信息 */
        private SessionConfigVO sessionConfigVO;
    }
}