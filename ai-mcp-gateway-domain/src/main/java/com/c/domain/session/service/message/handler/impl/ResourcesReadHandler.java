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
 * MCP 资源读取处理器
 * 处理resources/read请求，根据URI读取对应网关资源内容
 *
 * @author cyh
 * @date 2026/03/25
 */
@Slf4j
@Service("resourcesReadHandler")
@RequiredArgsConstructor
public class ResourcesReadHandler implements IRequestHandler {

    /** 网关配置仓储，用于查询网关基础配置 */
    private final GatewayRepository gatewayRepository;

    /**
     * 处理资源读取请求
     *
     * @param gatewayId 网关唯一标识
     * @param message   JSON-RPC请求消息
     * @return 资源内容响应
     */
    @Override
    public Flux<McpSchemaVO.JSONRPCResponse> handle(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        // 校验消息类型：仅处理Request请求
        if (!(message instanceof McpSchemaVO.JSONRPCRequest req)) {
            return Flux.error(new AppException("MCP-400", "resources/read 只能处理请求消息"));
        }

        // 查询网关配置，不存在则抛出异常
        McpGatewayConfigVO config = Optional
                .ofNullable(
                        gatewayRepository.queryMcpGatewayConfigByGatewayId(gatewayId)
                )
                .orElseThrow(() -> new AppException("MCP-404", "gateway config not found"));

        // 默认资源URI，支持从请求参数中覆盖
        String uri = "gateway://" + gatewayId + "/tools/" + config.getToolId();
        if (req.params() instanceof Map<?, ?> map) {
            Object u = map.get("uri");
            if (u != null) {
                uri = String.valueOf(u);
            }
        }

        // 构建资源内容
        Map<String, Object> content = new HashMap<>();
        content.put("uri", uri);
        content.put("mimeType", "text/plain");
        content.put("text", "Resource content for gateway=" + gatewayId);

        // 封装响应结果
        Map<String, Object> result = new HashMap<>();
        result.put("contents", List.of(content));

        return Flux.just(McpSchemaVO.JSONRPCResponse.ofSuccess(req.id(), result));
    }
}