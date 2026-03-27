package com.c.cases.mcp.message;

import com.c.cases.mcp.api.service.McpMessageService;
import com.c.cases.mcp.framework.tree.StrategyHandler;
import com.c.cases.mcp.message.factory.DefaultMcpMessageFactory;
import com.c.domain.session.adapter.repository.SessionSsePort;
import com.c.domain.session.model.entity.HandleMessageCommandEntity;
import com.c.domain.session.model.valobj.McpSchemaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * MCP 消息服务实现
 * 提供外部接口：处理上行指令消息、主动下行推送消息
 *
 * @author cyh
 * @date 2026/03/27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpMessageServiceImpl implements McpMessageService {

    /** 消息处理工厂，用于获取责任链入口 */
    private final DefaultMcpMessageFactory defaultMcpMessageFactory;
    /** SSE 连接存储与发送端口 */
    private final SessionSsePort sessionSsePort;

    /**
     * 处理客户端发来的 MCP 指令消息
     *
     * @param commandEntity 指令实体
     * @return HTTP 响应 Mono
     */
    @Override
    public Mono<ResponseEntity<Void>> handleMessage(HandleMessageCommandEntity commandEntity) {
        log.info("MCP原始消息: {}", commandEntity);
        log.info("接收 MCP 指令消息 | sessionId: {}", commandEntity.getSessionId());
        try {
            // 1. 从工厂获取责任链根处理器
            StrategyHandler<HandleMessageCommandEntity, DefaultMcpMessageFactory.DynamicContext,
                    Mono<ResponseEntity<Void>>> strategyHandler = defaultMcpMessageFactory.strategyHandler();

            // 2. 新建上下文并启动责任链执行
            return strategyHandler.apply(commandEntity, new DefaultMcpMessageFactory.DynamicContext());

        } catch (Exception e) {
            // 责任链启动阶段异常，直接返回 500
            log.error("MCP 指令链路执行失败", e);
            return Mono.just(ResponseEntity
                    .internalServerError()
                    .build());
        }
    }

    /**
     * 服务端主动向客户端推送消息
     *
     * @param sessionId 会话ID
     * @param message   消息对象
     * @return 完成信号 Mono
     */
    @Override
    public Mono<Void> pushMessage(String sessionId, Object message) {
        return Mono.fromRunnable(() -> {
            try {
                // 1. 判断是否已经是字符串，否则序列化
                String json = (message instanceof String) ? (String) message : McpSchemaVO.toJson(message);

                // 2. 构造 SSE 事件并通过端口发送
                sessionSsePort.send(sessionId, ServerSentEvent
                        .<String>builder()
                        .event("message")
                        .data(json)
                        .build());

                log.debug("主动推送消息成功 | sessionId: {}", sessionId);
            } catch (Exception e) {
                log.error("主动推送消息失败 | sessionId: {}", sessionId, e);
                throw new RuntimeException("Push failed", e);
            }
        });
    }
}