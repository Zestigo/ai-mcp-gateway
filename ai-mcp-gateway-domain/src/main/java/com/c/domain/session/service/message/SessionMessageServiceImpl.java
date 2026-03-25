package com.c.domain.session.service.message;

import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.enums.SessionMessageHandlerMethodEnum;
import com.c.domain.session.service.message.handler.IRequestHandler;
import com.c.types.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * MCP 会话消息处理服务实现类
 * 统一接收、解析、路由、处理 MCP 协议的 JSON-RPC 消息
 * 根据消息方法自动匹配对应处理器，并统一异常响应构建
 *
 * @author cyh
 * @date 2026/03/25
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SessionMessageServiceImpl implements SessionMessageService {

    /**
     * 消息处理器集合
     * Key：处理器名称，Value：对应的请求处理器实例
     */
    private final Map<String, IRequestHandler> handlers;

    /**
     * 统一处理 MCP 会话消息入口
     * 异步调度执行，自动路由到对应处理器，统一异常处理
     *
     * @param gatewayId 网关唯一标识
     * @param message   JSON-RPC 消息体（请求/通知）
     * @return JSON-RPC 响应流
     */
    @Override
    public Flux<McpSchemaVO.JSONRPCResponse> process(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        return Flux.defer(() -> {
                       // 解析消息并匹配对应的消息处理器
                       IRequestHandler handler = resolve(message);
                       // 执行处理器处理逻辑
                       return handler.handle(gatewayId, message);
                   })
                   // 绑定弹性线程池，异步执行消息处理
                   .subscribeOn(Schedulers.boundedElastic())
                   // 统一异常处理，构造标准错误响应
                   .onErrorResume(ex -> buildError(message, ex));
    }

    /**
     * 解析 JSON-RPC 消息类型并获取对应处理器
     * 支持 Request 和 Notification 两种消息类型
     *
     * @param message JSON-RPC 消息
     * @return 匹配到的请求处理器
     */
    private IRequestHandler resolve(McpSchemaVO.JSONRPCMessage message) {

        // 处理带 ID 的请求消息
        if (message instanceof McpSchemaVO.JSONRPCRequest req) {
            return getHandler(req.method());
        }

        // 处理不带 ID 的通知消息
        if (message instanceof McpSchemaVO.JSONRPCNotification noti) {
            return getHandler(noti.method());
        }

        // 不支持的消息类型
        throw new AppException("MCP-400", "invalid message");
    }

    /**
     * 根据方法名获取对应的处理器实例
     * 先通过枚举匹配，再从 Spring 注入的处理器集合中获取
     *
     * @param method JSON-RPC 方法名
     * @return 对应的请求处理器
     */
    private IRequestHandler getHandler(String method) {

        // 根据方法名获取枚举配置
        var enumVal = SessionMessageHandlerMethodEnum
                .getByMethod(method)
                .orElseThrow(() -> new AppException("MCP-404", "method not found"));

        // 根据处理器名称从处理器集合获取实例
        IRequestHandler handler = handlers.get(enumVal.getHandlerName());

        // 处理器不存在则抛出异常
        if (handler == null) {
            throw new AppException("MCP-404", "handler missing");
        }

        return handler;
    }

    /**
     * 统一构建异常响应
     * 仅对 Request 类型消息返回错误，Notification 不响应
     *
     * @param message 原始消息
     * @param ex      异常信息
     * @return 错误响应流
     */
    private Flux<McpSchemaVO.JSONRPCResponse> buildError(McpSchemaVO.JSONRPCMessage message, Throwable ex) {
        return Flux.defer(() -> {
            // 仅请求类型消息需要返回错误响应
            if (message instanceof McpSchemaVO.JSONRPCRequest req) {
                return Flux.just(McpSchemaVO.JSONRPCResponse.ofError(req.id(), McpSchemaVO.ErrorCodes.INTERNAL_ERROR,
                        ex.getMessage() == null ? "internal error" : ex.getMessage(), null));
            }
            // 通知类型消息不返回任何响应
            return Flux.empty();
        });
    }
}