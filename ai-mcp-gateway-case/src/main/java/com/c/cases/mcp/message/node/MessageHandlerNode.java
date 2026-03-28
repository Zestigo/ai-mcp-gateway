package com.c.cases.mcp.message.node;

import com.c.cases.mcp.framework.tree.StrategyHandler;
import com.c.cases.mcp.message.AbstractMcpMessageSupport;
import com.c.cases.mcp.message.factory.DefaultMcpMessageFactory;
import com.c.domain.session.model.entity.HandleMessageCommandEntity;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.SessionConfigVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * MCP消息处理业务节点
 * 负责将业务处理结果推送到SSE连接，完成消息闭环
 * 是消息处理链路的最终业务执行节点
 *
 * @author cyh
 * @date 2026/03/28
 */
@Slf4j
@Service("mcpMessageMessageHandlerNode")
public class MessageHandlerNode extends AbstractMcpMessageSupport {

    /**
     * 消息处理核心业务逻辑
     * 校验连接有效性，调用业务服务，推送结果至客户端
     *
     * @param request 消息处理命令实体
     * @param context 动态上下文
     * @return 响应结果异步对象
     */
    @Override
    protected Mono<ResponseEntity<Void>> doApply(HandleMessageCommandEntity request,
                                                 DefaultMcpMessageFactory.DynamicContext context) {

        SessionConfigVO vo = context.getSessionConfigVO();

        // 1. 快速失败：连接不存在直接返回410
        if (vo == null || vo.getSink() == null) {
            log.error("[处理异常] SSE管道不存在或已失效 | sessionId: {}", request.getSessionId());
            return Mono.just(ResponseEntity
                    .status(HttpStatus.GONE)
                    .build());
        }

        log.info("[消息处理] 开始转发至业务 Service | sessionId: {}", request.getSessionId());

        // 2. 调用领域服务处理业务逻辑
        return sessionMessageService
                .process(request.getGatewayId(), request.getJsonrpcMessage())
                .flatMap(response -> {
                    // 推送结果至SSE客户端
                    return pushToSseClient(vo.getSink(), response, request.getSessionId());
                })
                // 3. 流处理完成后返回202，兼容无响应场景
                .then(Mono.just(ResponseEntity
                        .accepted()
                        .<Void>build()))
                // 4. 全局异常统一返回500
                .onErrorResume(e -> {
                    log.error("[处理失败] 消息链路执行异常 | sessionId: {}", request.getSessionId(), e);
                    return Mono.just(ResponseEntity
                            .internalServerError()
                            .build());
                });
    }

    /**
     * 消息推送至SSE客户端
     * 序列化消息并通过Sink发送，处理推送状态与异常
     *
     * @param sink      SSE连接发送器
     * @param response  业务响应对象
     * @param sessionId 会话标识
     * @return 异步推送完成信号
     */
    private Mono<Void> pushToSseClient(Sinks.Many<ServerSentEvent<String>> sink, Object response, String sessionId) {
        return Mono.fromRunnable(() -> {
            try {
                // 序列化为JSON字符串
                String json = McpSchemaVO.toJson(response);

                // 构建SSE事件并尝试发送
                Sinks.EmitResult result = sink.tryEmitNext(ServerSentEvent
                        .<String>builder()
                        .event("message")
                        .data(json)
                        .build());

                // 处理发送结果
                if (result.isFailure()) {
                    log.error("[推送失败] Sink 状态异常: {} | sessionId: {}", result, sessionId);
                } else {
                    log.debug("[推送成功] 消息已进入 SSE 管道 | sessionId: {}", sessionId);
                }
            } catch (Exception e) {
                log.error("[序列化异常] 响应转换 JSON 失败", e);
            }
        });
    }

    /**
     * 获取责任链下一个执行节点
     * 业务处理完成，到达链路末端，返回默认处理器
     *
     * @param request 消息处理命令实体
     * @param context 动态上下文
     * @return 默认策略处理器
     */
    @Override
    public StrategyHandler<HandleMessageCommandEntity, DefaultMcpMessageFactory.DynamicContext,
            Mono<ResponseEntity<Void>>> get(HandleMessageCommandEntity request,
                                            DefaultMcpMessageFactory.DynamicContext context) {
        return defaultStrategyHandler;
    }
}