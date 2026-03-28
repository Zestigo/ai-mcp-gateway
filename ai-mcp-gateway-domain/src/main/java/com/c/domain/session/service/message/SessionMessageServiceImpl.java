package com.c.domain.session.service.message;

import com.alibaba.fastjson2.JSON;
import com.c.domain.session.adapter.repository.SessionRedisPort;
import com.c.domain.session.adapter.repository.SessionRepository;
import com.c.domain.session.adapter.repository.SessionSsePort;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.enums.SessionMessageHandlerMethodEnum;
import com.c.domain.session.service.SessionManagementService;
import com.c.domain.session.service.message.handler.IRequestHandler;
import com.c.types.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

import static com.c.domain.session.model.valobj.McpSchemaVO.ErrorCodes.INTERNAL_ERROR;

/**
 * 会话消息处理服务实现类
 *
 * @author cyh
 * @date 2026/03/27
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SessionMessageServiceImpl implements SessionMessageService {

    /** 请求处理器集合，key为处理器名称，value为对应处理器实例 */
    private final Map<String, IRequestHandler> handlers;

    /** 会话管理逻辑，用于会话Sink获取、会话生命周期管理等操作 */
    private final SessionManagementService sessionManagementService;

    /** 会话持久化仓储，负责会话数据的存储与查询 */
    private final SessionRepository sessionRepository;

    /** 本地SSE通讯端口，提供本地节点SSE连接管理与消息发送能力 */
    private final SessionSsePort sessionSsePort;

    /** Redis路由与分发端口，提供跨节点消息分发与会话关联管理能力 */
    private final SessionRedisPort sessionRedisPort;

    /**
     * 处理MCP协议消息，匹配对应处理器执行业务逻辑
     *
     * @param gatewayId 网关唯一标识
     * @param message   MCP协议消息体
     * @return 响应结果流
     */
    @Override
    public Flux<McpSchemaVO.JSONRPCResponse> process(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        return Flux.defer(() -> {
                       // 解析消息，获取对应请求处理器
                       IRequestHandler handler = resolve(message);
                       return handler
                               .handle(gatewayId, message)
                               .doOnNext(r -> log.debug("[处理器响应] 生成结果: {}", r));
                   })
                   // 切换至弹性线程池执行，避免阻塞主线程
                   .subscribeOn(Schedulers.boundedElastic())
                   // 异常统一处理，构建标准错误响应
                   .onErrorResume(ex -> buildError(message, ex));
    }

    /**
     * 推送消息至指定会话，支持本地SSE推送与跨节点Redis转发
     *
     * @param sessionId 会话唯一标识
     * @param message   MCP协议消息体
     * @return 推送完成信号
     */
    @Override
    public Mono<Void> push(String sessionId, McpSchemaVO.JSONRPCMessage message) {
        return Mono.justOrEmpty(sessionRepository.find(sessionId))
                   // 会话不存在时抛出业务异常
                   .switchIfEmpty(Mono.error(new AppException("MCP-404", "Session not found")))
                   .flatMap(session -> {
                       // 1. 检查本地是否存在SSE连接
                       var localSink = sessionSsePort.get(sessionId);

                       if (localSink != null) {
                           log.info("【本地推送】sessionId: {}, method: {}", sessionId, getMethod(message));
                           // 通过本地SSE端口发送消息
                           sessionSsePort.send(sessionId, ServerSentEvent
                                   .builder(JSON.toJSONString(message))
                                   .build());
                           return Mono.empty();
                       }

                       // 2. 本地无连接，通过Redis端口进行跨节点消息转发
                       return Mono.fromRunnable(() -> {
                                      log.info("【跨机转发】目标节点: {}, sessionId: {}", session.getHostIp(), sessionId);
                                      sessionRedisPort.publish(session.getHostIp(), sessionId, message);
                                  })
                                  // 异步执行转发操作
                                  .subscribeOn(Schedulers.boundedElastic())
                                  .then();
                   });
    }

    /**
     * 获取消息的方法名，兼容请求与通知两种消息类型
     *
     * @param message MCP协议消息体
     * @return 消息方法名，未知类型返回unknown
     */
    private String getMethod(McpSchemaVO.JSONRPCMessage message) {
        if (message instanceof McpSchemaVO.JSONRPCRequest req) return req.method();
        if (message instanceof McpSchemaVO.JSONRPCNotification noti) return noti.method();
        return "unknown";
    }

    /**
     * 解析消息，匹配对应的请求处理器
     *
     * @param message MCP协议消息体
     * @return 匹配的请求处理器
     * @throws AppException 方法无对应处理器时抛出
     */
    private IRequestHandler resolve(McpSchemaVO.JSONRPCMessage message) {
        String method = getMethod(message);
        return SessionMessageHandlerMethodEnum
                .getByMethod(method)
                .map(e -> handlers.get(e.getHandlerName()))
                .orElseThrow(() -> new AppException("MCP-404", "Method handler not found: " + method));
    }

    /**
     * 构建错误响应消息，仅处理请求类型消息
     *
     * @param message 原始请求消息
     * @param ex      异常信息
     * @return 错误响应流
     */
    private Flux<McpSchemaVO.JSONRPCResponse> buildError(McpSchemaVO.JSONRPCMessage message, Throwable ex) {
        if (message instanceof McpSchemaVO.JSONRPCRequest req) {
            return Flux.just(McpSchemaVO.JSONRPCResponse.ofError(req.id(), INTERNAL_ERROR, ex.getMessage(), null));
        }
        return Flux.empty();
    }
}