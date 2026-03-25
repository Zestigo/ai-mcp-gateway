package com.c.domain.session.service.message.handler;

import com.c.domain.session.model.valobj.McpSchemaVO;
import reactor.core.publisher.Flux;

/**
 * MCP JSON-RPC 请求处理器接口
 * 定义统一的消息处理规范，所有MCP消息处理器都需实现该接口
 *
 * @author cyh
 * @date 2026/03/25
 */
public interface IRequestHandler {

    /**
     * 统一处理MCP JSON-RPC消息
     *
     * @param gatewayId 网关唯一标识，用于区分不同网关实例
     * @param message   MCP协议消息体，支持Request和Notification类型
     * @return 响应结果流，通知类型返回空流
     */
    Flux<McpSchemaVO.JSONRPCResponse> handle(String gatewayId, McpSchemaVO.JSONRPCMessage message);
}