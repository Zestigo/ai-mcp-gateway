package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.adapter.repository.McpGatewayConfigRepository;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * resources/read 处理器
 *
 * @author cyh
 * @date 2026/03/24
 */
@Slf4j
@Service("resourcesReadHandler")
public class ResourcesReadHandler implements IRequestHandler {

    /** 网关配置仓储 */
    @Resource
    private McpGatewayConfigRepository gatewayConfigRepository;

    /**
     * 处理资源读取
     *
     * @param gatewayId 网关标识
     * @param message   请求消息
     * @return 响应结果
     */
    @Override
    public McpSchemaVO.JSONRPCResponse handle(String gatewayId, McpSchemaVO.JSONRPCRequest message) {
        // 查询网关配置
        McpGatewayConfigVO config = gatewayConfigRepository.queryMcpGatewayConfigByGatewayId(gatewayId);

        // 默认资源URI
        String uri = "gateway://" + gatewayId + "/tools/" + config.getToolId();
        // 从请求参数获取URI
        if (message.params() instanceof Map<?, ?> paramMap) {
            Object uriParam = paramMap.get("uri");
            if (uriParam != null) {
                uri = String.valueOf(uriParam);
            }
        }

        // 构造资源内容
        Map<String, Object> content = new HashMap<>();
        content.put("uri", uri);
        content.put("mimeType", "text/plain");
        content.put("text", "Resource content for gateway=" + gatewayId + ", tool=" + config.getToolName());

        // 包装响应
        Map<String, Object> result = new HashMap<>();
        result.put("contents", List.of(content));

        log.info("处理 resources/read | gatewayId={} | uri={}", gatewayId, uri);

        return new McpSchemaVO.JSONRPCResponse("2.0", message.id(), result, null);
    }
}