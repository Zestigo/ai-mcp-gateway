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
 * MCP 资源列表查询处理器
 * 处理resources/list请求，返回网关关联的可用资源列表
 *
 * @author cyh
 * @date 2026/03/25
 */
@Slf4j
@Service("resourcesListHandler")
@RequiredArgsConstructor
public class ResourcesListHandler implements IRequestHandler {

    /** 网关配置仓储，用于查询网关与工具配置 */
    private final GatewayRepository gatewayRepository;

    /**
     * 处理资源列表查询请求
     *
     * @param gatewayId 网关唯一标识
     * @param message   JSON-RPC请求消息
     * @return 网关资源列表响应
     */
    @Override
    public Flux<McpSchemaVO.JSONRPCResponse> handle(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        // 校验消息类型：仅处理Request请求
        if (!(message instanceof McpSchemaVO.JSONRPCRequest req)) {
            return Flux.error(new AppException("MCP-400", "resources/list 只能处理请求消息"));
        }

        // 查询网关配置，不存在则抛出异常
        McpGatewayConfigVO config = Optional
                .ofNullable(
                        gatewayRepository.queryMcpGatewayConfigByGatewayId(gatewayId)
                )
                .orElseThrow(() -> new AppException("MCP-404", "gateway config not found"));

        // 构建网关资源信息
        Map<String, Object> resource = new HashMap<>();
        resource.put("uri", "gateway://" + gatewayId + "/tools/" + config.getToolId());
        resource.put("name", config.getToolName());
        resource.put("description", config.getToolDescription());

        // 封装响应结果
        Map<String, Object> result = new HashMap<>();
        result.put("resources", List.of(resource));

        return Flux.just(McpSchemaVO.JSONRPCResponse.ofSuccess(req.id(), result));
    }
}