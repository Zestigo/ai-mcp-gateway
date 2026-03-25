package com.c.domain.session.service.message;

import com.c.domain.session.model.valobj.McpSchemaVO;
import reactor.core.publisher.Flux;

/**
 * 会话消息处理服务接口
 *
 * @author cyh
 * @date 2026/03/24
 */
public interface SessionMessageService {

    /**
     * 处理JSON-RPC消息
     *
     * @param gatewayId 网关标识
     * @param message   请求消息
     * @return 响应结果
     */
    Flux<McpSchemaVO.JSONRPCResponse> process(String gatewayId, McpSchemaVO.JSONRPCMessage message);
}