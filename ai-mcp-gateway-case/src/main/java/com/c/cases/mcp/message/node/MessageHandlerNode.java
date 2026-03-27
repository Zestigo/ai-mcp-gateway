package com.c.cases.mcp.message.node;

import com.c.cases.mcp.framework.tree.StrategyHandler;
import com.c.cases.mcp.message.AbstractMcpMessageSupport;
import com.c.cases.mcp.message.factory.DefaultMcpMessageFactory;
import com.c.domain.session.model.entity.HandleMessageCommandEntity;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.SessionConfigVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * MCP消息处理业务节点
 * 负责JSON-RPC消息业务处理、响应序列化、SSE异步推送
 * 作为消息责任链叶子节点完成最终处理
 *
 * @author cyh
 * @date 2026/03/27
 */
@Slf4j
@Service("mcpMessageMessageHandlerNode")
public class MessageHandlerNode extends AbstractMcpMessageSupport {

    /**
     * 执行消息处理核心逻辑
     *
     * @param request 消息处理命令实体
     * @param context 动态上下文
     * @return HTTP响应异步结果
     */
    @Override
    protected Mono<ResponseEntity<Void>> doApply(HandleMessageCommandEntity request,
                                                 DefaultMcpMessageFactory.DynamicContext context) {
        log.info("[消息处理] 开始处理消息 | request: {}", request);
        log.info("[上下文信息] 动态上下文内容: {}", context);

        // 从上下文获取会话配置，包含SSE推送Sink
        SessionConfigVO vo = context.getSessionConfigVO();

        // 校验SSE连接是否有效
        if (vo == null || vo.getSink() == null) {
            log.error("[处理异常] SSE连接不存在或已失效 | sessionId: {}", request.getSessionId());
            return Mono.just(ResponseEntity
                    .internalServerError()
                    .build());
        }
        log.info("[会话配置] 当前会话连接信息: {}", vo);

        // 调用业务服务处理消息
        return sessionMessageService
                .process(request.getGatewayId(), request.getJsonrpcMessage())
                .flatMap(response -> {
                    try {
                        // 序列化响应结果
                        String json = McpSchemaVO.toJson(response);
                        log.info("[响应推送] 即将推送到客户端的消息: {}", json);

                        // 通过SSE Sink异步推送给客户端
                        vo
                                .getSink()
                                .tryEmitNext(ServerSentEvent
                                        .<String>builder()
                                        .event("message")
                                        .data(json)
                                        .build());

                        // 返回202表示已接收并处理
                        return Mono.just(ResponseEntity
                                .accepted()
                                .<Void>build());
                    } catch (Exception e) {
                        log.error("[推送异常] 消息推送给客户端失败 | sessionId: {}", request.getSessionId(), e);
                        return Mono.error(e);
                    }
                })
                // 无论多少响应，最终只返回一次HTTP结果
                .last(ResponseEntity
                        .accepted()
                        .build())
                // 全局异常兜底
                .onErrorReturn(ResponseEntity
                        .internalServerError()
                        .build());
    }

    /**
     * 指定策略链下一级节点（叶子节点无后续节点）
     */
    @Override
    public StrategyHandler<HandleMessageCommandEntity, DefaultMcpMessageFactory.DynamicContext,
            Mono<ResponseEntity<Void>>> get(HandleMessageCommandEntity request,
                                            DefaultMcpMessageFactory.DynamicContext context) {
        return defaultStrategyHandler;
    }
}