package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.adapter.repository.SessionRepository;
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
 * MCP根路径列表查询处理器
 * 处理roots/list请求，返回网关授权的可访问根路径范围，遵循MCP协议规范
 *
 * @author cyh
 * @date 2026/03/26
 */
@Slf4j
@Service("rootsListHandler")
@RequiredArgsConstructor
public class RootsListHandler implements IRequestHandler {

    /** 会话领域仓储：负责会话生命周期管理及关联配置查询 */
    private final SessionRepository sessionRepository;

    /**
     * 处理MCP根路径查询请求
     *
     * @param gatewayId 网关唯一标识
     * @param message   JSON-RPC请求消息
     * @return 根路径列表响应结果
     */
    @Override
    public Flux<McpSchemaVO.JSONRPCResponse> handle(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        // 校验消息类型是否为合法请求
        if (!(message instanceof McpSchemaVO.JSONRPCRequest req)) {
            return Flux.error(new AppException("MCP-400", "roots/list 只能处理请求消息"));
        }

        // 查询并校验网关基础配置信息
        McpGatewayConfigVO config = Optional
                .ofNullable(sessionRepository.queryMcpGatewayConfigByGatewayId(gatewayId))
                .orElseThrow(() -> new AppException("MCP-404", "网关配置不存在或已禁用: " + gatewayId));

        // 构建MCP协议标准根路径信息
        Map<String, Object> root = new HashMap<>();
        root.put("uri", String.format("gateway://%s", gatewayId));
        root.put("name", config.getGatewayName());

        // 封装MCP协议标准响应结果
        Map<String, Object> result = new HashMap<>();
        result.put("roots", List.of(root));

        log.info("MCP_ROOTS_LIST | gatewayId={} | rootUri={}", gatewayId, root.get("uri"));

        // 返回成功响应
        return Flux.just(McpSchemaVO.JSONRPCResponse.ofSuccess(req.id(), result));
    }
}