package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.adapter.repository.GatewayConfigRepository;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 初始化请求处理器
 *
 * @author cyh
 * @date 2026/03/24
 */
@Slf4j
@Service("initializeHandler")
public class InitializeHandler implements IRequestHandler {

    /** 网关配置仓储 */
    @Resource
    private GatewayConfigRepository gatewayConfigRepository;

    /**
     * 处理初始化请求
     *
     * @param gatewayId 网关标识
     * @param message   请求消息
     * @return 响应结果
     */
    @Override
    public McpSchemaVO.JSONRPCResponse handle(String gatewayId, McpSchemaVO.JSONRPCRequest message) {
        // 查询网关配置
        McpGatewayConfigVO config = gatewayConfigRepository.queryMcpGatewayConfigByGatewayId(gatewayId);

        log.info("处理 initialize 请求 | gatewayId={}", gatewayId);

        // 构造标准初始化响应
        return new McpSchemaVO.JSONRPCResponse(
                "2.0",
                message.id(),
                Map.of(
                        "protocolVersion", "2024-11-05",
                        "capabilities", Map.of(
                                "tools", Map.of(),
                                "resources", Map.of(),
                                "logging", Map.of()
                        ),
                        "serverInfo", Map.of(
                                "name", config.getGatewayName(),
                                "version", config.getToolVersion()
                        )
                ),
                null
        );
    }
}