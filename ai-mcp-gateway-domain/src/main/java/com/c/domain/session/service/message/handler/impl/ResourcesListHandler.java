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
import java.util.stream.Collectors;

/**
 * MCP资源列表查询处理器
 * 处理resources/list类型请求，将网关下的工具转换为标准MCP资源列表返回
 *
 * @author cyh
 * @date 2026/03/26
 */
@Slf4j
@Service("resourcesListHandler")
@RequiredArgsConstructor
public class ResourcesListHandler implements IRequestHandler {

    /** 网关配置仓储 */
    private final GatewayRepository gatewayRepository;

    /**
     * 处理MCP资源列表查询请求
     *
     * @param gatewayId 网关唯一标识
     * @param message   JSON-RPC请求消息
     * @return 资源列表响应结果
     */
    @Override
    public Flux<McpSchemaVO.JSONRPCResponse> handle(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        // 校验消息类型是否为合法请求
        if (!(message instanceof McpSchemaVO.JSONRPCRequest req)) {
            return Flux.error(new AppException("MCP-400", "resources/list 只能处理请求消息"));
        }

        log.info("resources/list | gatewayId={}", gatewayId);

        // 查询当前网关下所有工具配置信息
        List<McpToolConfigVO> toolConfigs = gatewayRepository.queryMcpGatewayToolConfigListByGatewayId(gatewayId);

        // 将工具配置转换为MCP协议标准资源格式
        List<Map<String, Object>> resources = toolConfigs
                .stream()
                .map(tool -> {
                    Map<String, Object> resource = new HashMap<>();
                    resource.put("uri", String.format("mcp://%s/resources/%s", gatewayId, tool.getToolId()));
                    resource.put("name", tool.getToolName());
                    resource.put("description", tool.getToolDescription());
                    resource.put("mimeType", "application/json");
                    return resource;
                })
                .collect(Collectors.toList());

        // 封装MCP协议标准响应结果
        Map<String, Object> result = new HashMap<>();
        result.put("resources", resources);

        // 返回成功响应
        return Flux.just(McpSchemaVO.JSONRPCResponse.ofSuccess(req.id(), result));
    }
}