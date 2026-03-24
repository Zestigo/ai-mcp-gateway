package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.adapter.repository.GatewayConfigRepository;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * roots/list 处理器
 *
 * @author cyh
 * @date 2026/03/24
 */
@Slf4j
@Service("rootsListHandler")
public class RootsListHandler implements IRequestHandler {

    /** 网关配置仓储 */
    @Resource
    private GatewayConfigRepository gatewayConfigRepository;

    /**
     * 处理根目录查询
     *
     * @param gatewayId 网关标识
     * @param message   请求消息
     * @return 响应结果
     */
    @Override
    public McpSchemaVO.JSONRPCResponse handle(String gatewayId, McpSchemaVO.JSONRPCRequest message) {
        // 查询网关配置
        McpGatewayConfigVO config = gatewayConfigRepository.queryMcpGatewayConfigByGatewayId(gatewayId);

        // 构造根目录信息
        Map<String, Object> root = Map.of("uri", "/api-gateway", "name", config.getGatewayName());

        // 包装响应
        Map<String, Object> result = Map.of("roots", List.of(root));

        log.info("处理 roots/list | gatewayId={}", gatewayId);

        return new McpSchemaVO.JSONRPCResponse("2.0", message.id(), result, null);
    }
}