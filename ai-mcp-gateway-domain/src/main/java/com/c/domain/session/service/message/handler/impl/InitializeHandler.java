package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.adapter.repository.GatewayRepository;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import com.c.domain.session.model.valobj.gateway.McpToolConfigVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import com.c.types.exception.AppException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * MCP初始化请求处理器
 * 处理客户端与服务端的初始化握手，完成身份校验、协议协商与服务端能力声明
 *
 * @author cyh
 * @date 2026/03/26
 */
@Slf4j
@Service("initializeHandler")
@RequiredArgsConstructor
public class InitializeHandler implements IRequestHandler {

    /** 网关配置仓储 */
    private final GatewayRepository gatewayRepository;

    /**
     * 处理MCP初始化请求
     *
     * @param gatewayId 网关ID
     * @param message   JSON-RPC消息
     * @return 初始化响应结果
     */
    @Override
    public Flux<McpSchemaVO.JSONRPCResponse> handle(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        // 校验消息类型是否为合法请求
        if (!(message instanceof McpSchemaVO.JSONRPCRequest req)) {
            return Flux.error(new AppException("MCP-400", "Initialize 必须为请求类型消息"));
        }

        // 解析初始化请求参数
        McpSchemaVO.InitializeRequest initializeRequest = McpSchemaVO.convert(req.params(),
                new TypeReference<>() {
                });
        if (initializeRequest == null) {
            return Flux.error(new AppException("MCP-400", "Initialize 请求参数解析失败"));
        }

        log.info("MCP_INIT_START | gatewayId={} | clientVersion={}", gatewayId, initializeRequest.protocolVersion());

        // 查询并校验网关基础配置信息
        McpGatewayConfigVO config = Optional
                .ofNullable(gatewayRepository.queryMcpGatewayConfigByGatewayId(gatewayId))
                .orElseThrow(() -> new AppException("MCP-404", "网关未授权或不存在: " + gatewayId));

        // 查询网关下可用工具配置
        List<McpToolConfigVO> tools = gatewayRepository.queryMcpGatewayToolConfigListByGatewayId(gatewayId);
        if (tools == null || tools.isEmpty()) {
            log.warn("MCP_INIT_WARNING | gatewayId={} | 原因: 当前网关未配置任何可用工具(Tools为空)", gatewayId);
        }

        // 协商协议版本，优先使用客户端版本
        String protocolVersion = StringUtils.hasText(initializeRequest.protocolVersion()) ?
                initializeRequest.protocolVersion() : McpSchemaVO.LATEST_PROTOCOL_VERSION;

        // 构建服务端能力声明
        McpSchemaVO.ServerCapabilities capabilities =
                new McpSchemaVO.ServerCapabilities(new McpSchemaVO.ServerCapabilities.CompletionCapabilities(),
                        Collections.emptyMap(), new McpSchemaVO.ServerCapabilities.LoggingCapabilities(),
                        new McpSchemaVO.ServerCapabilities.PromptCapabilities(true),
                        new McpSchemaVO.ServerCapabilities.ResourceCapabilities(true, true),
                        new McpSchemaVO.ServerCapabilities.ToolCapabilities(true));

        // 构建初始化响应结果
        McpSchemaVO.InitializeResult result = new McpSchemaVO.InitializeResult(protocolVersion, capabilities,
                new McpSchemaVO.Implementation(config.getGatewayName(), config.getGatewayVersion()),
                config.getGatewayDescription());

        log.info("MCP_INIT_SUCCESS | gatewayId={} | version={}", gatewayId, protocolVersion);

        // 返回成功响应
        return Flux.just(McpSchemaVO.JSONRPCResponse.ofSuccess(req.id(), result));
    }
}