package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 初始化完成处理器：处理notifications/initialized指令
 *
 * @author cyh
 * @date 2026/03/23
 */
@Slf4j
@Service("initializedHandler")
public class InitializedHandler implements IRequestHandler {

    /**
     * 处理客户端初始化完成通知
     *
     * @param message JSON-RPC请求对象
     * @return null（通知类请求无响应）
     */
    @Override
    public McpSchemaVO.JSONRPCResponse handle(McpSchemaVO.JSONRPCRequest message) {
        log.info("MCP握手完成 | 客户端已initialized");
        // 扩展点：标记会话为就绪状态
        // sessionService.markReady(message);
        return null; // notification类型不返回响应
    }
}