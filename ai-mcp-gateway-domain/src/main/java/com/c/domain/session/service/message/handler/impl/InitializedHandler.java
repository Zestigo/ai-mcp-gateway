package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import com.c.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * MCP 初始化完成通知处理器
 * 处理客户端发送的 initialized 通知消息，标记MCP握手流程完成
 *
 * @author cyh
 * @date 2026/03/25
 */
@Slf4j
@Service("initializedHandler")
public class InitializedHandler implements IRequestHandler {

    /**
     * 处理MCP初始化完成通知
     *
     * @param gatewayId 网关唯一标识
     * @param message   JSON-RPC通知消息
     * @return 空响应流（通知无需返回数据）
     */
    @Override
    public Flux<McpSchemaVO.JSONRPCResponse> handle(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        // 校验消息类型：仅支持通知消息
        if (message instanceof McpSchemaVO.JSONRPCNotification notification) {
            log.info("MCP 握手完成 | gatewayId={} | method={}", gatewayId, notification.method());
            return Flux.empty();
        }

        // 非通知消息，抛出参数异常
        return Flux.error(new AppException("MCP-400", "initialized 只能是通知消息"));
    }
}