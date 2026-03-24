package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.adapter.repository.GatewayConfigRepository;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * tools/list 处理器
 *
 * @author cyh
 * @date 2026/03/24
 */
@Slf4j
@Service("toolsListHandler")
public class ToolsListHandler implements IRequestHandler {

    /** 网关配置仓储 */
    @Resource
    private GatewayConfigRepository gatewayConfigRepository;

    /**
     * 处理工具列表查询
     *
     * @param gatewayId 网关标识
     * @param message   请求消息
     * @return 响应结果
     */
    @Override
    public McpSchemaVO.JSONRPCResponse handle(String gatewayId, McpSchemaVO.JSONRPCRequest message) {
        // 查询网关配置
        McpGatewayConfigVO config = gatewayConfigRepository.queryMcpGatewayConfigByGatewayId(gatewayId);

        // 构造工具信息
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", config.getToolName());
        tool.put("description", config.getToolDesc());
        tool.put("version", config.getToolVersion());

        // 包装响应
        Map<String, Object> result = new HashMap<>();
        result.put("tools", new ArrayList<>(java.util.List.of(tool)));

        log.info("处理 tools/list | gatewayId={}", gatewayId);

        return new McpSchemaVO.JSONRPCResponse("2.0", message.id(), result, null);
    }
}