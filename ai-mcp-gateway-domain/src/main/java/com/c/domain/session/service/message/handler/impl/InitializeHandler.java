package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.adapter.repository.GatewayRepository;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import com.c.types.exception.AppException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.Optional;

/**
 * MCP 初始化请求处理器
 * 处理客户端与服务端的initialize握手流程，返回服务端能力、版本与基础配置信息
 *
 * @author cyh
 * @date 2026/03/25
 */
@Slf4j
@Service("initializeHandler")
@RequiredArgsConstructor
public class InitializeHandler implements IRequestHandler {

    /** 网关配置仓储，用于查询MCP网关与工具的核心配置信息 */
    private final GatewayRepository gatewayRepository;

    /**
     * 处理MCP初始化握手请求
     *
     * @param gatewayId 网关唯一标识
     * @param message   JSON-RPC请求消息
     * @return 初始化结果响应，包含服务端能力与版本信息
     */
    @Override
    public Flux<McpSchemaVO.JSONRPCResponse> handle(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        // 校验消息类型：仅处理Request请求
        if (!(message instanceof McpSchemaVO.JSONRPCRequest req)) {
            return Flux.error(new AppException("MCP-400", "initialize 只能处理请求消息"));
        }
        log.info("initialize | gatewayId={} | params={}", gatewayId, McpSchemaVO.toJson(req.params()));

        // 将请求参数转换为初始化请求对象，并校验非空
        McpSchemaVO.InitializeRequest initializeRequest = McpSchemaVO.convert(req.params(), new TypeReference<>() {
        });
        if (initializeRequest == null) {
            return Flux.error(new AppException("MCP-400", "initialize params 不能为空"));
        }

        // 根据网关ID查询配置信息，配置不存在则抛出404异常
        McpGatewayConfigVO config = Optional
                .ofNullable(gatewayRepository.queryMcpGatewayConfigByGatewayId(gatewayId))
                .orElseThrow(() -> new AppException("MCP-404", "gateway config not found"));

        // 协议版本兼容：使用客户端传入版本，无则使用默认最新版本
        String protocolVersion = StringUtils.hasText(initializeRequest.protocolVersion()) ?
                initializeRequest.protocolVersion() : McpSchemaVO.LATEST_PROTOCOL_VERSION;

        // 构建初始化响应结果：协议版本+服务端能力+实现信息+描述
        McpSchemaVO.InitializeResult result = new McpSchemaVO.InitializeResult(protocolVersion,
                new McpSchemaVO.ServerCapabilities(new McpSchemaVO.ServerCapabilities.CompletionCapabilities(),
                        Collections.emptyMap(), new McpSchemaVO.ServerCapabilities.LoggingCapabilities(),
                        new McpSchemaVO.ServerCapabilities.PromptCapabilities(true),
                        new McpSchemaVO.ServerCapabilities.ResourceCapabilities(false, true),
                        new McpSchemaVO.ServerCapabilities.ToolCapabilities(true)),
                new McpSchemaVO.Implementation(config.getToolName(), config.getToolVersion()),
                config.getToolDescription());

        // 返回初始化成功响应
        return Flux.just(McpSchemaVO.JSONRPCResponse.ofSuccess(req.id(), result));
    }
}