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

/**
 * MCP网关HTTP接口
 * 提供SSE长连接建立、JSON-RPC消息接收与处理能力
 *
 * @author cyh
 * @date 2026/03/27
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/gateways")
public class McpGatewayController {

    /** 会话服务 */
    private final McpSessionService mcpSessionService;

    /** 消息处理服务 */
    private final McpMessageService mcpMessageService;

    /**
     * 建立SSE长连接入口
     *
     * @param gatewayId 网关唯一标识
     * @return SSE事件流
     */
    @GetMapping(value = "/{gatewayId}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> establishSSEConnection(@PathVariable String gatewayId) {
        log.info("[HTTP请求] 接收SSE连接建立请求 | gatewayId: {}", gatewayId);

        // 参数校验
        if (!StringUtils.hasText(gatewayId)) {
            return Flux.error(new IllegalArgumentException("gatewayId 不能为空"));
        }

        return mcpSessionService
                .createMcpSession(gatewayId)
                .doOnSubscribe(s -> log.debug("[SSE订阅] 连接订阅已激活 | gatewayId: {}", gatewayId))
                .doOnError(e -> log.error("[SSE异常] 连接建立失败 | gatewayId: {}", gatewayId, e));
    }

    /**
     * 接收并处理MCP JSON-RPC消息
     *
     * @param gatewayId   网关唯一标识
     * @param sessionId   会话唯一标识
     * @param messageBody JSON-RPC原始报文
     * @return HTTP响应结果
     */
    @PostMapping("/{gatewayId}/sessions/{sessionId}/messages")
    public Mono<ResponseEntity<Void>> handleMessage(@PathVariable String gatewayId, @PathVariable String sessionId,
                                                    @RequestBody(required = false) String messageBody) {

        log.debug("[消息接收] sessionId: {}, body: {}", sessionId, messageBody);

        // 消息体非空校验
        if (!StringUtils.hasText(messageBody)) {
            return Mono.just(ResponseEntity
                    .badRequest()
                    .build());
        }

        return Mono
                // 反序列化消息，仅做格式校验
                .fromCallable(() -> McpSchemaVO.deserialize(messageBody))
                // 构造领域命令对象
                .map(jsonrpcMessage -> HandleMessageCommandEntity
                        .builder()
                        .gatewayId(gatewayId)
                        .sessionId(sessionId)
                        .jsonrpcMessage(jsonrpcMessage)
                        .build())
                // 交给消息服务处理
                .flatMap(mcpMessageService::handleMessage)
                .doOnSuccess(res -> log.debug("[消息处理完成] sessionId: {}", sessionId))
                .onErrorResume(e -> {
                    log.warn("[消息处理失败] sessionId: {}, 错误信息: {}", sessionId, e.getMessage());
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