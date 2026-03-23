package com.c.trigger.http;

import com.c.api.service.McpGatewayService;
import com.c.cases.mcp.api.service.McpSessionService;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.service.message.ISessionMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * MCP网关接口控制器
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
    private final ISessionMessageService serviceMessageService;

    /**
     * 建立SSE长连接
     *
     * @param gatewayId 网关标识
     * @return SSE事件流
     */
    @Override
    @GetMapping(value = "/{gatewayId}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> establishSSEConnection(@PathVariable("gatewayId") String gatewayId) {
        // 创建会话并返回SSE流
        return mcpSessionService.createMcpSession(gatewayId);
    }

    /**
     * 接收MCP消息并异步处理
     *
     * @param gatewayId   网关标识
     * @param sessionId   会话标识
     * @param messageBody 消息内容
     * @return 响应结果
     */
    @Override
    @PostMapping(value = "/{gatewayId}/sessions/{sessionId}/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Void>> handleMessage(@PathVariable("gatewayId") String gatewayId, @PathVariable(
            "sessionId") String sessionId, @RequestBody String messageBody) {

        log.info("接收MCP指令 | gatewayId:{} | sessionId:{}", gatewayId, sessionId);

        return Mono
                // 解析JSON-RPC消息
                .fromCallable(() -> McpSchemaVO.deserializeJsonRpcMessage(messageBody))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(jsonrpcMessage -> {
                    // 是请求消息则异步处理
                    if (jsonrpcMessage instanceof McpSchemaVO.JSONRPCRequest request) {
                        executeAsyncAndPush(gatewayId, sessionId, request);
                    }
                    // 立即返回202接受
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.ACCEPTED)
                            .<Void>build());
                })
                .onErrorResume(e -> {
                    // 解析异常返回400
                    log.error("指令解析失败", e);
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .build());
                });
    }

    /**
     * 异步执行业务并通过SSE推送结果
     *
     * @param gatewayId 网关标识
     * @param sessionId 会话标识
     * @param request   请求对象
     */
    private void executeAsyncAndPush(String gatewayId, String sessionId, McpSchemaVO.JSONRPCRequest request) {
        serviceMessageService
                .processHandlerMessage(gatewayId, request)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        // 成功处理
                        response -> {
                            // 通知消息无需推送
                            if (request.isNotification()) {
                                log.info("通知处理完成 | method={}", request.method());
                                return;
                            }
                            log.info("业务完成推送结果 | method={} | id={}", request.method(), request.id());
                            // 推送结果到SSE
                            mcpSessionService.pushMessage(sessionId, McpSchemaVO.toJson(response));
                        },
                        // 处理异常
                        ex -> {
                            // 通知消息异常仅打印日志
                            if (request.isNotification()) {
                                log.warn("通知处理异常 | method={} | error={}", request.method(), ex.getMessage(), ex);
                                return;
                            }
                            log.error("业务执行异常 | method={} | id={}", request.method(), request.id(), ex);
                            // 构造错误响应并推送
                            var error = McpSchemaVO.buildErrorResponse(request.id(), -32603, ex.getMessage() == null
                                    ? "Internal error" : ex.getMessage());
                            mcpSessionService.pushMessage(sessionId, McpSchemaVO.toJson(error));
                        });
    }
}