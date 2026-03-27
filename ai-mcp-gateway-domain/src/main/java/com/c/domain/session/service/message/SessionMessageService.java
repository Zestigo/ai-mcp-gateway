package com.c.domain.session.service.message;

import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.SessionConfigVO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 会话消息处理服务接口
 * 定义会话生命周期内JSON-RPC消息处理、客户端消息主动推送的核心能力
 *
 * @author cyh
 * @date 2026/03/27
 */
public interface SessionMessageService {

    /**
     * 处理网关传入的JSON-RPC协议消息
     * 支持请求、通知、响应等多种消息类型的分发与业务处理
     *
     * @param gatewayId 网关唯一标识
     * @param message   JSON-RPC协议消息体
     * @return 处理后的异步响应结果流
     */
    Flux<McpSchemaVO.JSONRPCResponse> process(String gatewayId, McpSchemaVO.JSONRPCMessage message);

    /**
     * 主动向指定会话客户端推送协议消息
     * 用于服务端主动下发执行结果、状态通知等场景
     *
     * @param sessionId 会话唯一标识
     * @param message   待推送的JSON-RPC协议消息
     * @return 推送完成异步信号
     */
    Mono<Void> push(String sessionId, McpSchemaVO.JSONRPCMessage message);
}