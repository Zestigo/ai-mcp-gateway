package com.c.domain.session.service.message.handler;

import com.c.domain.session.model.valobj.McpSchemaVO;

/**
 * RPC请求处理器接口，定义处理JSON-RPC请求的统一规范
 *
 * @author cyh
 * @date 2026/03/20
 */
public interface IRequestHandler {

    /**
     * 处理JSON-RPC请求并返回响应
     *
     * @param message JSON-RPC请求消息体
     * @return JSON-RPC响应结果
     */
    McpSchemaVO.JSONRPCResponse handle(McpSchemaVO.JSONRPCRequest message);

}