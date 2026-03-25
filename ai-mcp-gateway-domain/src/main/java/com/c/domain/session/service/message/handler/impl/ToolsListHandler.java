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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MCP 工具列表查询处理器
 * 处理tools/list请求，返回网关下可用的工具列表信息
 *
 * @author cyh
 * @date 2026/03/25
 */
@Slf4j
@Service("toolsListHandler")
@RequiredArgsConstructor
public class ToolsListHandler implements IRequestHandler {

    /** 网关配置仓储，用于查询工具基础信息 */
    private final GatewayRepository gatewayRepository;

    /**
     * 处理工具列表查询请求
     *
     * @param gatewayId 网关唯一标识
     * @param message   JSON-RPC请求消息
     * @return 工具列表响应
     */
    @Override
    public Flux<McpSchemaVO.JSONRPCResponse> handle(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        // 校验消息类型：仅处理Request请求
        if (!(message instanceof McpSchemaVO.JSONRPCRequest req)) {
            return Flux.error(new AppException("MCP-400", "tools/list 只能处理请求消息"));
        }

        // 查询网关配置，不存在则抛出异常
        McpGatewayConfigVO config = Optional
                .ofNullable(gatewayRepository.queryMcpGatewayConfigByGatewayId(gatewayId))
                .orElseThrow(() -> new AppException("MCP-404", "gateway config not found"));

        // 构建工具信息
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", config.getToolName());
        tool.put("description", config.getToolDescription());
        tool.put("version", config.getToolVersion());

        // 封装响应结果
        Map<String, Object> result = new HashMap<>();
        result.put("tools", List.of(tool));

        return Flux.just(McpSchemaVO.JSONRPCResponse.ofSuccess(req.id(), result));
    }
}