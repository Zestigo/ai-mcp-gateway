package com.c.domain.session.service.message;

import com.alibaba.fastjson2.JSON;
import com.c.domain.session.model.entity.McpSession;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.enums.SessionMessageHandlerMethodEnum;
import com.c.domain.session.service.SessionManagementService;
import com.c.domain.session.service.message.handler.IRequestHandler;
import com.c.types.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * 会话消息处理服务实现类
 * 负责MCP协议JSON-RPC消息的分发处理、本地/跨节点消息推送、异常统一封装
 * 基于请求方法自动匹配处理器，支持分布式环境下的会话消息路由
 * 核心流程：消息解析 -> 处理器匹配 -> 本地/远程推送 -> 异常响应构建
 *
 * @author cyh
 * @date 2026/03/27
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SessionMessageServiceImpl implements SessionMessageService {

    /** 请求处理器集合，Key：处理器名称，Value：处理器实例 */
    private final Map<String, IRequestHandler> handlers;

    /** 会话管理服务，用于会话查询、本地Sink获取等操作 */
    private final SessionManagementService sessionManagementService;

    /** Redis操作模板，用于跨节点消息发布 */
    private final StringRedisTemplate stringRedisTemplate;

    /** Redis消息队列主题前缀，拼接宿主机IP实现节点定向推送 */
    private static final String MCP_TOPIC_PREFIX = "mcp_node_";

    /**
     * 处理JSON-RPC协议消息
     * 自动解析请求方法并匹配对应处理器，异步执行并统一异常处理
     * 执行流程：延迟创建流 -> 解析处理器 -> 执行处理逻辑 -> 异步调度 -> 异常封装
     *
     * @param gatewayId 网关唯一标识，用于链路追踪
     * @param message   JSON-RPC消息对象，包含请求/通知类型
     * @return 响应结果流，包含处理成功的响应数据
     */
    @Override
    public Flux<McpSchemaVO.JSONRPCResponse> process(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        return Flux.defer(() -> {
                       // 解析并获取对应消息处理器，根据消息方法匹配
                       IRequestHandler handler = resolve(message);
                       return handler
                               .handle(gatewayId, message)
                               .doOnNext(r -> log.debug("[处理器响应] 生成响应结果: {}", r))
                               .doOnComplete(() -> log.debug("[处理器完成] 消息处理执行完成"))
                               .doOnError(e -> log.error("[处理器异常] 消息处理执行失败", e));
                   })
                   // 绑定弹性线程池，异步执行消息处理，避免阻塞主线程
                   .subscribeOn(Schedulers.boundedElastic())
                   // 全局异常处理，统一构建错误响应返回
                   .onErrorResume(ex -> buildError(message, ex));
    }

    /**
     * 主动向客户端推送消息
     * 优先本地Sink推送，本地无连接时通过Redis发布至目标节点处理
     * 执行流程：查询会话 -> 校验会话存在性 -> 本地推送/远程转发
     *
     * @param sessionId 会话唯一标识，用于定位目标会话
     * @param message   待推送的JSON-RPC消息
     * @return 推送完成异步信号，无返回数据
     */
    @Override
    public Mono<Void> push(String sessionId, McpSchemaVO.JSONRPCMessage message) {
        return sessionManagementService.getSession(sessionId)
                                       // 会话不存在时，抛出会话未找到自定义异常
                                       .switchIfEmpty(Mono.error(new AppException("MCP-404", "Session not found")))
                                       .flatMap(session -> {
                                           // 获取本地会话Sink，判断是否为当前节点连接的会话
                                           var localSinkOpt = sessionManagementService.getLocalSink(sessionId);

                                           // 本地会话：直接通过Sink推送消息至客户端
                                           if (localSinkOpt.isPresent()) {
                                               log.info("【本地推送】sessionId: {}, method: {}", sessionId,
                                                       getMethod(message));
                                               localSinkOpt
                                                       .get()
                                                       .tryEmitNext(ServerSentEvent
                                                               .builder(JSON.toJSONString(message))
                                                               .build());
                                               return Mono.empty();
                                           }

                                           // 远程会话：转发至目标宿主机节点处理推送
                                           return dispatchToRemoteNode(session, message);
                                       });
    }

    /**
     * 跨节点消息转发
     * 将消息封装后发布至Redis指定主题，由目标宿主机节点消费并推送
     * 异步执行Redis发布操作，不阻塞主线程
     *
     * @param session 会话实体，包含会话ID、宿主机IP等信息
     * @param message 待转发的协议消息
     * @return 转发完成异步信号，无返回数据
     */
    private Mono<Void> dispatchToRemoteNode(McpSession session, McpSchemaVO.JSONRPCMessage message) {
        return Mono.fromRunnable(() -> {
                       // 拼接Redis目标主题，格式：mcp_node_目标节点IP
                       String targetTopic = MCP_TOPIC_PREFIX + session.getHostIp();
                       log.info("【跨机转发】目标节点: {}, sessionId: {}", session.getHostIp(), session.getSessionId());

                       // 封装跨机传输对象，保证消息结构完整可序列化
                       RemotePushMessage remoteMsg = new RemotePushMessage(session.getSessionId(), message);
                       stringRedisTemplate.convertAndSend(targetTopic, JSON.toJSONString(remoteMsg));
                   })
                   // 异步执行Redis发布，使用弹性线程池
                   .subscribeOn(Schedulers.boundedElastic())
                   .then();
    }

    /**
     * 从消息体中提取method字段
     * 兼容请求、通知两种JSON-RPC消息类型
     * 非指定类型时返回默认值unknown
     *
     * @param message JSON-RPC消息对象
     * @return 消息方法名，无对应类型返回unknown
     */
    private String getMethod(McpSchemaVO.JSONRPCMessage message) {
        if (message instanceof McpSchemaVO.JSONRPCRequest req) return req.method();
        if (message instanceof McpSchemaVO.JSONRPCNotification noti) return noti.method();
        return "unknown";
    }

    /**
     * 根据消息方法解析对应的请求处理器
     * 通过枚举匹配方法与处理器名称，从处理器集合中获取实例
     *
     * @param message JSON-RPC消息对象
     * @return 匹配的请求处理器实例
     * @throws AppException 方法未匹配到处理器时抛出异常
     */
    private IRequestHandler resolve(McpSchemaVO.JSONRPCMessage message) {
        String method = getMethod(message);
        return SessionMessageHandlerMethodEnum
                .getByMethod(method)
                .map(e -> handlers.get(e.getHandlerName()))
                .orElseThrow(() -> new AppException("MCP-404", "Method not found"));
    }

    /**
     * 统一构建错误响应
     * 仅对带ID的请求消息返回错误响应，通知类无ID消息不返回响应
     * 标准化JSON-RPC错误格式
     *
     * @param message 原始请求/通知消息
     * @param ex      处理过程中抛出的异常信息
     * @return 封装后的错误响应流，无匹配类型返回空流
     */
    private Flux<McpSchemaVO.JSONRPCResponse> buildError(McpSchemaVO.JSONRPCMessage message, Throwable ex) {
        if (message instanceof McpSchemaVO.JSONRPCRequest req) {
            return Flux.just(McpSchemaVO.JSONRPCResponse.ofError(req.id(), -32603, ex.getMessage(), null));
        }
        return Flux.empty();
    }

    /**
     * 跨节点消息推送传输对象
     * 用于Redis发布/订阅场景下的消息封装
     * 实现序列化传输，保证跨节点消息结构完整
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class RemotePushMessage {
        /** 目标会话唯一标识 */
        private String sessionId;
        /** 待推送的JSON-RPC协议消息 */
        private McpSchemaVO.JSONRPCMessage message;
    }
}