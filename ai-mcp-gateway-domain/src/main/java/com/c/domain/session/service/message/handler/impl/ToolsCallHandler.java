package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.adapter.repository.GatewayRepository;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import com.c.types.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * MCP 工具调用处理器
 * 处理tools/call请求，接收客户端工具调用参数并返回处理结果
 *
 * @author cyh
 * @date 2026/03/25
 */
@Slf4j
@Service("toolsCallHandler")
@RequiredArgsConstructor
public class ToolsCallHandler implements IRequestHandler {

    /** 网关配置仓储，用于查询工具配置信息 */
    private final GatewayRepository gatewayRepository;

    /**
     * 处理工具调用请求
     *
     * @param gatewayId 网关唯一标识
     * @param message   JSON-RPC请求消息
     * @return 工具调用结果响应
     */
    @Override
    public Flux<McpSchemaVO.JSONRPCResponse> handle(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        // 校验消息类型：仅处理Request请求
        if (!(message instanceof McpSchemaVO.JSONRPCRequest req)) {
            return Flux.error(new AppException("MCP-400", "tools/call 只能处理请求消息"));
        }

        // 查询网关配置，不存在则抛出异常
        McpGatewayConfigVO config = Optional
                .ofNullable(gatewayRepository.queryMcpGatewayConfigByGatewayId(gatewayId))
                .orElseThrow(() -> new AppException("MCP-404", "gateway config not found"));

        // 解析工具调用参数
        Map<String, Object> arguments = new HashMap<>();
        if (req.params() instanceof Map<?, ?> map) {
            map.forEach((k, v) -> arguments.put(String.valueOf(k), v));
        } else if (req.params() != null) {
            arguments.put("value", req.params());
        }

        // 构建工具调用响应结果
        Map<String, Object> result = new HashMap<>();
        result.put("gatewayId", gatewayId);
        result.put("toolId", config.getToolId());
        result.put("toolName", config.getToolName());
        result.put("arguments", arguments);
        result.put("status", "accepted");

        return Flux.just(McpSchemaVO.JSONRPCResponse.ofSuccess(req.id(), result));
    }
}