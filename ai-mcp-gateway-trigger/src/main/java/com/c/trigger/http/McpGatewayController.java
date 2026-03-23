package com.c.trigger.http;

import com.c.api.IMcpGatewayService;
import com.c.cases.mcp.api.service.IMcpSessionService;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.service.ISessionMessageService;
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
 * MCP网关HTTP控制器：处理SSE连接建立、MCP指令接收
 *
 * @author cyh
 * @date 2026/03/23
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/gateways")
public class McpGatewayController implements IMcpGatewayService {

    private final IMcpSessionService mcpSessionService; // 会话管理服务
    private final ISessionMessageService serviceMessageService; // 消息处理服务

    /**
     * 建立MCP SSE长连接
     *
     * @param gatewayId 网关ID
     * @return SSE事件流
     */
    @GetMapping(value = "/{gatewayId}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> establishSSEConnection(@PathVariable("gatewayId") String gatewayId) {
        return mcpSessionService.createMcpSession(gatewayId);
    }

    /**
     * 接收MCP指令并异步处理
     *
     * @param gatewayId   网关ID
     * @param sessionId   会话ID
     * @param messageBody 指令消息体
     * @return 202 Accepted响应
     */
    @PostMapping(value = "/{gatewayId}/sessions/{sessionId}/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Void>> handleMessage(@PathVariable("gatewayId") String gatewayId, @PathVariable(
            "sessionId") String sessionId, @RequestBody String messageBody) {

        log.info("接收MCP指令 | gatewayId:{} | sessionId:{}", gatewayId, sessionId);

        return Mono
                .fromCallable(() -> McpSchemaVO.deserializeJsonRpcMessage(messageBody))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(jsonrpcMessage -> {
                    // 异步处理并推送结果
                    if (jsonrpcMessage instanceof McpSchemaVO.JSONRPCRequest request) {
                        executeAsyncAndPush(sessionId, request);
                    }
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.ACCEPTED)
                            .<Void>build());
                })
                .onErrorResume(e -> {
                    log.error("指令解析失败", e);
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .build());
                });
    }

    /**
     * 异步执行业务逻辑并推送结果
     *
     * @param sessionId 会话ID
     * @param request   JSON-RPC请求对象
     */
    private void executeAsyncAndPush(String sessionId, McpSchemaVO.JSONRPCRequest request) {
        serviceMessageService
                .processHandlerMessage(request)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(response -> {
                    // 通知类请求不推送响应
                    if (request.isNotification()) {
                        log.info("通知处理完成 | method: {}", request.method());
                        return;
                    }
                    // 推送正常响应
                    log.info("业务完成推送结果 | method: {} | id: {}", request.method(), request.id());
                    mcpSessionService.pushMessage(sessionId, McpSchemaVO.toJson(response));
                }, ex -> {
                    // 通知类请求异常不推送
                    if (request.isNotification()) {
                        log.warn("通知处理异常 | method: {} | error: {}", request.method(), ex.getMessage(), ex);
                        return;
                    }
                    // 推送错误响应
                    var error = McpSchemaVO.buildErrorResponse(request.id(), -32603, ex.getMessage() == null ?
                            "Internal error" : ex.getMessage());
                    mcpSessionService.pushMessage(sessionId, McpSchemaVO.toJson(error));
                });
    }
}