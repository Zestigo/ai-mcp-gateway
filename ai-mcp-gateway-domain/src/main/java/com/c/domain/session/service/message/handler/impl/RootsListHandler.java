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
 * MCP 根路径列表查询处理器
 * 处理roots/list请求，返回网关对应的根路径信息
 *
 * @author cyh
 * @date 2026/03/25
 */
@Slf4j
@Service("rootsListHandler")
@RequiredArgsConstructor
public class RootsListHandler implements IRequestHandler {

    /** 网关配置仓储，用于查询网关名称等配置 */
    private final GatewayRepository gatewayRepository;

    /**
     * 处理根路径列表查询请求
     *
     * @param gatewayId 网关唯一标识
     * @param message   JSON-RPC请求消息
     * @return 网关根路径列表响应
     */
    @Override
    public Flux<McpSchemaVO.JSONRPCResponse> handle(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        // 校验消息类型：仅处理Request请求
        if (!(message instanceof McpSchemaVO.JSONRPCRequest req)) {
            return Flux.error(new AppException("MCP-400", "roots/list 只能处理请求消息"));
        }

        // 查询网关配置，不存在则抛出异常
        McpGatewayConfigVO config = Optional
                .ofNullable(
                        gatewayRepository.queryMcpGatewayConfigByGatewayId(gatewayId)
                )
                .orElseThrow(() -> new AppException("MCP-404", "gateway config not found"));

        // 构建根路径信息
        Map<String, Object> root = new HashMap<>();
        root.put("uri", "/api-gateway");
        root.put("name", config.getGatewayName());

        // 封装响应结果
        Map<String, Object> result = new HashMap<>();
        result.put("roots", List.of(root));

        log.info("处理 roots/list | gatewayId={}", gatewayId);

        return Flux.just(McpSchemaVO.JSONRPCResponse.ofSuccess(req.id(), result));
    }
}