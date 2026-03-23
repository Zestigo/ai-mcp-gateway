package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 初始化完成通知处理器
 *
 * @author cyh
 * @date 2026/03/24
 */
@Slf4j
@Service("initializedHandler")
public class InitializedHandler implements IRequestHandler {

    /**
     * 处理初始化完成通知
     *
     * @param gatewayId 网关标识
     * @param message   请求消息
     * @return 响应结果
     */
    @Override
    public McpSchemaVO.JSONRPCResponse handle(String gatewayId, McpSchemaVO.JSONRPCRequest message) {
        // 仅记录日志，无需返回响应
        log.info("MCP 握手完成 | gatewayId={} | 客户端已 initialized", gatewayId);
        return null;
    }
}