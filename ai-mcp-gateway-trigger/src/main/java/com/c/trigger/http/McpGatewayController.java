package com.c.trigger.http;

import com.c.cases.mcp.api.service.McpMessageService;
import com.c.cases.mcp.api.service.McpSessionService;
import com.c.domain.session.model.entity.HandleMessageCommandEntity;
import com.c.domain.session.model.valobj.McpSchemaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * MCP网关HTTP接口
 * 提供HTTP协议接入层，处理SSE连接与消息请求
 * 适配浏览器与移动端标准调用方式
 *
 * @author cyh
 * @date 2026/03/28
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/gateways")
public class McpGatewayController {

    /** 会话服务，负责SSE连接管理 */
    private final McpSessionService mcpSessionService;

    /** 消息服务，负责MCP协议消息处理 */
    private final McpMessageService mcpMessageService;

    /**
     * 建立SSE长连接接口
     * 兼容Header与Query参数传递ApiKey，适配多端调用
     *
     * @param gatewayId    网关唯一标识
     * @param headerApiKey 请求头传递的密钥
     * @param queryApiKey  请求参数传递的密钥
     * @return SSE事件流
     */
    @GetMapping(value = "/{gatewayId}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> establishSSEConnection(@PathVariable String gatewayId, @RequestHeader(value
                                                                        = "X-Api-Key", required = false) String headerApiKey,
                                                                @RequestParam(value = "api_key", required = false) String queryApiKey) {

        // 优先从请求头获取ApiKey
        String apiKey = Optional
                .ofNullable(headerApiKey)
                .orElse(queryApiKey);

        // 基础参数校验
        if (!StringUtils.hasText(gatewayId) || !StringUtils.hasText(apiKey)) {
            return Flux.error(new IllegalArgumentException("Invalid gatewayId or api_key"));
        }
        log.info("[SSE请求] 接收连接建立请求 | gatewayId: {}", gatewayId);

        // 调用服务创建会话，添加异常处理
        return mcpSessionService
                .createMcpSession(gatewayId, apiKey)
                .doOnCancel(() -> log.info("[SSE关闭] 用户主动断开 | gatewayId: {}", gatewayId))
                .onErrorResume(e -> {
                    log.error("[SSE异常] gatewayId: {}, 错误: {}", gatewayId, e.getMessage());
                    return Flux.just(ServerSentEvent
                            .<String>builder()
                            .event("error")
                            .data("Internal Server Error")
                            .build());
                });
    }

    /**
     * 接收并处理JSON-RPC消息报文
     * 解析消息体，构建命令实体，分发至业务服务处理
     *
     * @param gatewayId    网关唯一标识
     * @param sessionId    会话唯一标识
     * @param headerApiKey 请求头密钥
     * @param queryApiKey  请求参数密钥
     * @param messageBody  消息原始内容
     * @return 处理结果响应
     */
    @PostMapping("/{gatewayId}/sessions/{sessionId}/messages")
    public Mono<ResponseEntity<Void>> handleMessage(@PathVariable String gatewayId, @PathVariable String sessionId,
                                                    @RequestHeader(value = "X-Api-Key", required = false) String headerApiKey, @RequestParam(value = "api_key", required = false) String queryApiKey, @RequestBody(required = false) String messageBody) {

        // 获取ApiKey
        String apiKey = Optional
                .ofNullable(headerApiKey)
                .orElse(queryApiKey);
        log.debug("[消息接收] gatewayId: {}, sessionId: {}, bodyLen: {}", gatewayId, sessionId, Optional
                .ofNullable(messageBody)
                .map(String::length)
                .orElse(0));

        // 快速失败校验
        if (!StringUtils.hasText(messageBody) || !StringUtils.hasText(apiKey)) {
            return Mono.just(ResponseEntity
                    .badRequest()
                    .build());
        }

        // 解析消息并构建命令实体
        return Mono
                .fromCallable(() -> McpSchemaVO.deserialize(messageBody))
                .map(jsonrpc -> HandleMessageCommandEntity
                        .builder()
                        .gatewayId(gatewayId)
                        .apiKey(apiKey)
                        .sessionId(sessionId)
                        .jsonrpcMessage(jsonrpc)
                        .build())
                .flatMap(mcpMessageService::handleMessage)
                .doOnSuccess(res -> log.info("[消息处理成功] sessionId: {}", sessionId))
                // 异常分类处理
                .onErrorResume(e -> {
                    log.error("[消息处理异常] sessionId: {}, reason: {}", sessionId, e.getMessage());
                    if (e instanceof IllegalArgumentException) {
                        return Mono.just(ResponseEntity
                                .badRequest()
                                .build());
                    }
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build());
                });
    }
}