package com.c.cases.mcp.message.node;

import com.c.cases.mcp.framework.tree.StrategyHandler;
import com.c.cases.mcp.message.AbstractMcpMessageSupport;
import com.c.cases.mcp.message.factory.DefaultMcpMessageFactory;
import com.c.domain.session.model.entity.HandleMessageCommandEntity;
import com.c.domain.session.model.valobj.SessionConfigVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 会话校验节点
 * 修正点：将 Context 填充逻辑完全闭包化，确保响应式链路完整性
 *
 * @author cyh
 * @date 2026/03/27
 */
@Slf4j
@Service("mcpMessageSessionNode")
public class SessionNode extends AbstractMcpMessageSupport {

    @Resource(name = "mcpMessageMessageHandlerNode")
    private MessageHandlerNode messageHandlerNode;

    @Override
    protected Mono<ResponseEntity<Void>> doApply(HandleMessageCommandEntity request,
                                                 DefaultMcpMessageFactory.DynamicContext context) {
        log.info(">>> [SessionNode] 准备校验会话 | sessionId: {}", request.getSessionId());

        return sessionManagementService
                .getSession(request.getSessionId())
                .flatMap(session -> {
                    // 1. 获取本地内存 Sink
                    return sessionManagementService
                            .getLocalSink(session.getSessionId())
                            .map(sink -> {
                                // 2. 核心：填充 Context
                                context.setSessionConfigVO(SessionConfigVO.from(session, sink));
                                log.info(">>> [SessionNode] Context 组装完成，准备路由下游节点");

                                // 3. 【解决异常报错】使用 Mono.defer 处理带有 throws 的方法
                                try {
                                    return router(request, context);
                                } catch (Exception e) {
                                    log.error(">>> [SessionNode] 路由分发失败", e);
                                    return Mono.<ResponseEntity<Void>>error(e);
                                }
                            })
                            .orElseGet(() -> {
                                // 4. Sink 丢失的调试埋点
                                log.error(">>> [SessionNode] SSE Sink 丢失！请求 ID: [{}] | 内存池状态: {}",
                                        session.getSessionId(), sessionManagementService.getAllLocalKeys());
                                return Mono.just(ResponseEntity
                                        .status(HttpStatus.GONE)
                                        .build());
                            });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn(">>> [SessionNode] 无法识别的会话 ID: {}", request.getSessionId());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.UNAUTHORIZED)
                            .build());
                }));
    }

    @Override
    public StrategyHandler<HandleMessageCommandEntity, DefaultMcpMessageFactory.DynamicContext,
            Mono<ResponseEntity<Void>>> get(HandleMessageCommandEntity request,
                                            DefaultMcpMessageFactory.DynamicContext context) {
        return messageHandlerNode;
    }
}