package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.adapter.repository.GatewayConfigRepository;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * tools/call 处理器
 *
 * @author cyh
 * @date 2026/03/24
 */
@Slf4j
@Service("toolsCallHandler")
public class ToolsCallHandler implements IRequestHandler {

    /** 网关配置仓储 */
    @Resource
    private GatewayConfigRepository gatewayConfigRepository;

    /**
     * 处理工具调用
     *
     * @param gatewayId 网关标识
     * @param message   请求消息
     * @return 响应结果
     */
    @Override
    public McpSchemaVO.JSONRPCResponse handle(String gatewayId, McpSchemaVO.JSONRPCRequest message) {
        // 查询网关配置
        McpGatewayConfigVO config = gatewayConfigRepository.queryMcpGatewayConfigByGatewayId(gatewayId);

        // 构造调用结果
        Map<String, Object> result = new HashMap<>();
        result.put("gatewayId", gatewayId);
        result.put("toolId", config.getToolId());
        result.put("toolName", config.getToolName());
        result.put("arguments", message.params());
        result.put("status", "accepted");

        log.info("处理 tools/call | gatewayId={} | tool={}", gatewayId, config.getToolName());

        return new McpSchemaVO.JSONRPCResponse("2.0", message.id(), result, null);
    }
}