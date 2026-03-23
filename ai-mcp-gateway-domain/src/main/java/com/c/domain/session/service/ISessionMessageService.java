package com.c.domain.session.service;

import com.c.domain.session.model.valobj.McpSchemaVO;
import reactor.core.publisher.Mono;

/**
 * 会话消息处理服务接口
 * 该接口定义了会话消息处理的核心能力，主要用于处理基于 JSON-RPC 协议的会话消息交互
 * 负责接收客户端的 JSON-RPC 请求，并返回对应的处理响应
 *
 * @author cyh
 * @date 2026/03/19
 */
public interface ISessionMessageService {

    /**
     * 处理会话处理器消息
     *
     * @param message JSON-RPC 格式的会话请求消息，包含请求方法、参数、ID 等核心信息
     * @return JSON-RPC 格式的处理响应消息，包含处理结果、错误信息（如有）、对应的请求 ID 等
     */
    Mono<McpSchemaVO.JSONRPCResponse> processHandlerMessage(McpSchemaVO.JSONRPCRequest message);

}