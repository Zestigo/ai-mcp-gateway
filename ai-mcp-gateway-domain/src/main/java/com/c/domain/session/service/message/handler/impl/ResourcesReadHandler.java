package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.adapter.repository.GatewayRepository;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.gateway.McpToolConfigVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import com.c.types.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP资源读取请求处理器
 * 处理resources/read类型请求，根据资源URI解析工具ID并返回对应工具的详细配置信息
 *
 * @author cyh
 * @date 2026/03/26
 */
@Slf4j
@Service("resourcesReadHandler")
@RequiredArgsConstructor
public class ResourcesReadHandler implements IRequestHandler {

    /** 网关配置仓储 */
    private final GatewayRepository gatewayRepository;

    /**
     * 处理MCP资源读取请求
     *
     * @param gatewayId 网关唯一标识
     * @param message   JSON-RPC请求消息
     * @return 资源内容响应结果
     */
    @Override
    public Flux<McpSchemaVO.JSONRPCResponse> handle(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        // 校验消息类型是否为合法请求
        if (!(message instanceof McpSchemaVO.JSONRPCRequest req)) {
            return Flux.error(new AppException("MCP-400", "resources/read 只能处理请求消息"));
        }

        // 从请求参数中解析资源URI
        String uri = null;
        if (req.params() instanceof Map<?, ?> map) {
            uri = String.valueOf(map.get("uri"));
        }

        // 校验URI非空
        if (uri == null || uri.isEmpty()) {
            return Flux.error(new AppException("MCP-400", "URI 不能为空"));
        }

        log.info("resources/read | gatewayId={} | uri={}", gatewayId, uri);

        // 从URI格式中解析工具ID
        String toolId = parseToolIdFromUri(uri);
        if (toolId == null) {
            return Flux.error(new AppException("MCP-400", "无效的资源 URI 格式: " + uri));
        }

        // 查询网关下所有工具配置
        List<McpToolConfigVO> toolConfigs = gatewayRepository.queryMcpGatewayToolConfigListByGatewayId(gatewayId);

        // 匹配目标工具信息
        McpToolConfigVO targetTool = toolConfigs
                .stream()
                .filter(t -> t
                        .getToolId()
                        .toString()
                        .equals(toolId))
                .findFirst()
                .orElseThrow(() -> new AppException("MCP-404", "找不到指定的资源内容: " + toolId));

        // 构建资源响应内容
        Map<String, Object> content = new HashMap<>();
        content.put("uri", uri);
        content.put("mimeType", "application/json");
        content.put("text", String.format("Tool Config: Name=%s, Desc=%s, Version=%s", targetTool.getToolName(),
                targetTool.getToolDescription(), targetTool.getToolVersion()));

        // 封装MCP协议标准响应结果
        Map<String, Object> result = new HashMap<>();
        result.put("contents", List.of(content));

        // 返回成功响应
        return Flux.just(McpSchemaVO.JSONRPCResponse.ofSuccess(req.id(), result));
    }

    /**
     * 从资源URI中解析工具ID
     *
     * @param uri 资源唯一标识
     * @return 工具ID
     */
    private String parseToolIdFromUri(String uri) {
        try {
            String[] parts = uri.split("/");
            return parts[parts.length - 1];
        } catch (Exception e) {
            return null;
        }
    }
}