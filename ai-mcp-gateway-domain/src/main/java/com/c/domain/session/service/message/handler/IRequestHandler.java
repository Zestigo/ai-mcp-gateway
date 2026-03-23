package com.c.domain.session.service.message.handler;

import com.c.domain.session.model.valobj.McpSchemaVO;

/**
 * JSON-RPC 处理器接口
 *
 * @author cyh
 * @date 2026/03/24
 */
public interface IRequestHandler {

    /**
     * 处理JSON-RPC请求
     *
     * @param gatewayId 网关标识
     * @param message   请求消息
     * @return 响应结果
     */
    McpSchemaVO.JSONRPCResponse handle(String gatewayId, McpSchemaVO.JSONRPCRequest message);
}