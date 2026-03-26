package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.adapter.port.SessionPort;
import com.c.domain.session.adapter.repository.GatewayRepository;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.gateway.McpGatewayProtocolConfigVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import com.c.types.enums.ResponseCode;
import com.c.types.exception.AppException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

/**
 * MCP工具调用请求处理器
 * 处理tools/call类型请求，完成协议解析、配置加载、接口调用与响应封装
 *
 * @author cyh
 * @date 2026/03/26
 */
@Slf4j
@Service("toolsCallHandler")
@RequiredArgsConstructor
public class ToolsCallHandler implements IRequestHandler {

    /** 网关配置仓储 */
    private final GatewayRepository gatewayRepository;

    /** 会话服务端口 */
    private final SessionPort sessionPort;

    /**
     * 处理MCP工具调用请求
     *
     * @param gatewayId 网关ID
     * @param message   JSON-RPC消息
     * @return 响应结果流
     */
    @Override
    public Flux<McpSchemaVO.JSONRPCResponse> handle(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        if (!(message instanceof McpSchemaVO.JSONRPCRequest req)) {
            return Flux.error(new AppException(ResponseCode.ILLEGAL_PARAMETER));
        }

        // 使用 defer 包装，确保逻辑在订阅时执行
        return Flux
                .defer(() -> {
                    // 1. 同步准备配置（非阻塞）
                    McpGatewayProtocolConfigVO protocolConfig = Optional
                            .ofNullable(gatewayRepository.queryMcpGatewayProtocolConfig(gatewayId))
                            .orElseThrow(() -> new AppException(ResponseCode.DATA_NOT_FOUND));

                    McpSchemaVO.CallToolRequest callToolRequest = McpSchemaVO.convert(req.params(),
                            new TypeReference<>() {
                            });

                    // 2. 将阻塞的 toolCall 包装进 Mono，并切换到弹性线程池
                    return Mono
                            .fromCallable(() -> sessionPort.toolCall(protocolConfig.getHttpConfig(),
                                    callToolRequest.arguments()))
                            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()) // 解决阻塞问题的关键
                            .map(result -> {
                                // 3. 封装成功响应
                                Map<String, Object> payload = Map.of("content", new Object[]{Map.of("type", "text",
                                        "text", String.valueOf(result))}, "isError", false);
                                return McpSchemaVO.JSONRPCResponse.ofSuccess(req.id(), payload);
                            });
                })
                .onErrorResume(e -> {
                    // 4. 统一异常处理（在这里 IOException 已经被自动包装并传递过来了）
                    log.error("MCP 调用失败, gatewayId: {}", gatewayId, e);
                    int code = (e instanceof AppException ae) ? Integer.parseInt(ae.getCode()) : -32603;
                    return Flux.just(McpSchemaVO.JSONRPCResponse.ofError(req.id(), code, e.getMessage(), null));
                });
    }
}