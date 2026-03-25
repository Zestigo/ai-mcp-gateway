package com.c.trigger.http;

import com.c.api.service.McpGatewayService;
import com.c.cases.mcp.api.service.McpSessionService;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.service.message.SessionMessageService;
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

import java.io.IOException;

/**
 * MCP 网关接口控制器
 *
 * @author cyh
 * @date 2026/03/24
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/gateways")
public class McpGatewayController implements McpGatewayService {

    /** 会话服务 */
    private final McpSessionService mcpSessionService;

    /** 消息处理服务 */
    private final SessionMessageService sessionMessageService;

    /**
     * 建立 SSE 长连接
     *
     * @param gatewayId 网关标识
     * @return SSE 事件流
     */
    @Override
    @GetMapping(value = "/{gatewayId}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> establishSSEConnection(@PathVariable("gatewayId") String gatewayId) {
        return mcpSessionService.createMcpSession(gatewayId);
    }

    /**
     * 异步执行业务并通过 SSE 推送结果
     *
     * @param gatewayId   网关标识
     * @param sessionId   会话标识
     * @param messageBody 请求体
     */
    @PostMapping("/{gatewayId}/sessions/{sessionId}/messages")
    public Mono<ResponseEntity<Void>> handleMessage(@PathVariable("gatewayId") String gatewayId, @PathVariable(
            "sessionId") String sessionId, @RequestBody(required = false) String messageBody) {

        if (!StringUtils.hasText(messageBody)) {
            return Mono.just(ResponseEntity
                    .badRequest()
                    .build());
        }

        return Mono
                .fromCallable(() -> McpSchemaVO.deserialize(messageBody))
                .flatMapMany(message -> sessionMessageService.process(gatewayId, message))
                .doOnNext(resp -> mcpSessionService.pushMessage(sessionId, resp))
                .then(Mono.fromSupplier(() -> ResponseEntity
                        .accepted()
                        .<Void>build()))
                .onErrorResume(ex -> {
                    Throwable cause = ex;
                    while (cause.getCause() != null && cause != cause.getCause()) {
                        cause = cause.getCause();
                    }

                    if (cause instanceof IllegalArgumentException || cause instanceof IOException) {
                        log.warn("MCP 消息解析失败 | gatewayId={} | sessionId={} | err={}", gatewayId, sessionId, ex, ex);
                        return Mono.just(ResponseEntity
                                .badRequest()
                                .build());
                    }

                    log.error("MCP 消息处理失败 | gatewayId={} | sessionId={} | err={}", gatewayId, sessionId, ex, ex);
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build());
                });
    }
}