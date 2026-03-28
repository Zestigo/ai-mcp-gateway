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
 * 职责：从存储层/内存加载会话元数据与物理 Sink，并完成 Context 的响应式闭包填充
 * 负责会话有效性、连接活性校验，是消息处理的前置核心节点
 *
 * @author cyh
 * @date 2026/03/28
 */
@Slf4j
@Service("mcpMessageSessionNode")
public class SessionNode extends AbstractMcpMessageSupport {

    /** 消息处理节点，会话校验通过后流转至此 */
    @Resource(name = "mcpMessageMessageHandlerNode")
    private MessageHandlerNode messageHandlerNode;

    /**
     * 执行会话校验核心逻辑
     * 校验会话存在性、Sink活性，填充上下文后路由至消息处理器
     *
     * @param request 消息处理命令实体
     * @param context 动态上下文，存储中间处理数据
     * @return 响应结果异步对象，包含HTTP状态码
     */
    @Override
    protected Mono<ResponseEntity<Void>> doApply(HandleMessageCommandEntity request,
                                                 DefaultMcpMessageFactory.DynamicContext context) {
        String sessionId = request.getSessionId();
        log.info(">>> [SessionNode] 准备校验会话活性 | sessionId: {}", sessionId);

        // 1. 获取会话基本信息，从分布式存储/DB加载
        return sessionManagementService
                .getSession(sessionId)
                .flatMap(session -> Mono
                        .justOrEmpty(sessionManagementService.getLocalSink(sessionId))
                        .flatMap(sink -> {
                            // 3. 填充上下文：确认会话与连接均有效
                            context.setSessionConfigVO(SessionConfigVO.from(session, sink));
                            log.debug(">>> [SessionNode] 上下文填充成功，准备分发消息");

                            // 4. 执行路由，使用defer捕获同步异常
                            return Mono.defer(() -> {
                                try {
                                    return router(request, context);
                                } catch (Exception e) {
                                    return Mono.error(e);
                                }
                            });
                        })
                        // 5. Sink不存在：连接已断开，返回410
                        .switchIfEmpty(Mono.defer(() -> {
                            log.error(">>> [SessionNode] 物理管道(Sink)丢失 | sessionId: {} | 节点可能已漂移", sessionId);
                            return Mono.just(ResponseEntity
                                    .status(HttpStatus.GONE)
                                    .build());
                        })))
                // 6. 会话不存在或已过期，返回401
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn(">>> [SessionNode] 无法识别或已过期的会话 | sessionId: {}", sessionId);
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.UNAUTHORIZED)
                            .build());
                }))
                // 7. 未知异常统一返回500，防止链路中断
                .onErrorResume(e -> {
                    log.error(">>> [SessionNode] 会话校验过程触发异常 | sessionId: {}", sessionId, e);
                    return Mono.just(ResponseEntity
                            .internalServerError()
                            .build());
                });
    }

    /**
     * 获取责任链下一个执行节点
     * 固定路由至消息处理节点，形成标准处理流程
     *
     * @param request 消息处理命令实体
     * @param context 动态上下文
     * @return 下一个节点的策略处理器
     */
    @Override
    public StrategyHandler<HandleMessageCommandEntity, DefaultMcpMessageFactory.DynamicContext,
            Mono<ResponseEntity<Void>>> get(HandleMessageCommandEntity request,
                                            DefaultMcpMessageFactory.DynamicContext context) {
        return messageHandlerNode;
    }
}