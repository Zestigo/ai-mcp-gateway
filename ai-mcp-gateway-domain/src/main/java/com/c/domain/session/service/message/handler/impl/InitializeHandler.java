package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * RPC请求处理器-协议初始化，处理客户端与服务器的握手连接请求
 *
 * @author cyh
 * @date 2026/03/20
 */
@Slf4j
@Service("initializeHandler")
public class InitializeHandler implements IRequestHandler {

    @Override
    public McpSchemaVO.JSONRPCResponse handle(McpSchemaVO.JSONRPCRequest message) {
        // 记录初始化请求处理日志
        log.info("模拟处理初始化请求");

        // 返回初始化响应，包含协议版本、服务端能力、服务端信息
        return new McpSchemaVO.JSONRPCResponse("2.0", message.id(), Map.of("protocolVersion", "2024-11-05",
                "capabilities", Map.of("tools", Map.of(), "resources", Map.of()), "serverInfo", Map.of("name", "MCP " +
                        "Weather Proxy Server", "version", "1.0.0")), null);
    }

}