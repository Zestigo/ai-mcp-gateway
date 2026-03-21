package com.c.trigger.http;

import com.alibaba.fastjson.JSON;
import com.c.api.IMcpGatewayService;
import com.c.cases.mcp.api.service.IMcpSessionService;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.service.ISessionMessageService;
import com.c.types.enums.ResponseCode;
import com.c.types.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * MCP网关服务接口控制器
 * 核心能力：提供MCP网关SSE长连接建立、消息收发处理的REST接口
 * 技术特性：基于响应式编程（Reactor）实现非阻塞IO，支持跨域访问
 *
 * @author cyh
 * @date 2026/03/21
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT
        , RequestMethod.DELETE, RequestMethod.OPTIONS})
@RequestMapping("/api/v1/gateways")
public class McpGatewayController implements IMcpGatewayService {

    /** MCP会话服务：负责SSE会话的创建、维护与销毁 */
    private final IMcpSessionService mcpSessionService;
    /** 会话消息服务：负责MCP JSON-RPC消息的解析、处理与结果返回 */
    private final ISessionMessageService serviceMessageService;

    /**
     * 建立MCP网关的SSE长连接
     * SSE用于服务端向客户端单向推送实时消息，适配MCP网关实时通信场景
     *
     * @param gatewayId 网关唯一标识，不能为空或空白字符串
     * @return Flux<ServerSentEvent < String>> SSE事件流，持续推送网关相关消息
     * @throws AppException 网关ID为空时抛出非法参数异常（ResponseCode.ILLEGAL_PARAMETER）
     */
    @Override
    @GetMapping(value = "/{gatewayId}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> establishSSEConnection(@PathVariable("gatewayId") String gatewayId) {
        return Mono
                // 包装网关ID，为空则触发异常逻辑
                .justOrEmpty(gatewayId)
                // 过滤空/空白字符串，仅保留有效网关ID
                .filter(StringUtils::isNotBlank)
                // 空值兜底：网关ID为空时抛出非法参数异常
                .switchIfEmpty(Mono.error(() -> new AppException(ResponseCode.ILLEGAL_PARAMETER)))
                // 日志记录：SSE连接建立准备
                .doOnNext(id -> log.info("准备建立MCP SSE连接 | 网关ID: {}", id))
                // 转换为事件流：创建MCP会话并返回SSE事件
                .flatMapMany(mcpSessionService::createMcpSession)
                // 异常日志：记录SSE连接过程中的异常信息
                .doOnError(e -> log.error("建立MCP SSE连接异常 | 网关ID: {}", gatewayId, e))
                // 终止日志：记录SSE连接进程结束（正常/异常终止均触发）
                .doOnTerminate(() -> log.debug("MCP SSE连接进程结束 | 网关ID: {}", gatewayId));
    }

    /**
     * 处理MCP网关会话的消息收发
     * 接收JSON格式的MCP消息，解析为JSON-RPC请求并处理，返回统一格式响应
     *
     * @param sessionId   会话唯一标识
     * @param messageBody JSON格式的消息体（遵循JSON-RPC协议）
     * @return Mono<ResponseEntity < Object>> 响应式响应对象：
     * 成功：200 OK，返回{status: success, timestamp: 时间戳}
     * 失败：500 Internal Server Error，返回{error: 错误信息}
     */
    @PostMapping(value = "/sessions/{sessionId}/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> handleMessage(@PathVariable String sessionId, @RequestBody String messageBody) {
        // 使用fromCallable包装同步逻辑：确保代码在订阅阶段执行，避免主线程阻塞！
        return Mono.fromCallable(() -> {
                       log.info("处理MCP消息 | sessionId:{}", sessionId);
                       // 反序列化：将JSON字符串转为JSON-RPC消息对象
                       McpSchemaVO.JSONRPCMessage msg = McpSchemaVO.deserializeJsonRpcMessage(messageBody);

                       // 类型校验：仅处理JSON-RPC请求类型，非请求类型抛非法参数异常!
                       if (!(msg instanceof McpSchemaVO.JSONRPCRequest request)) {
                           throw new AppException(ResponseCode.ILLEGAL_PARAMETER);
                       }

                       // 核心处理：调用消息服务处理JSON-RPC请求
                       return serviceMessageService.processHandlerMessage(request);
                   })
                   // 成功响应：封装统一的成功返回格式
                   .map(response -> {
                       log.info("MCP消息处理成功 | 结果类型: {}", response
                               .getClass()
                               .getSimpleName());
                       return ResponseEntity.ok((Object) Map.of("status", "success", "timestamp",
                               System.currentTimeMillis()));
                   })
                   // 异常兜底：捕获所有处理异常，返回500错误响应
                   .onErrorResume(e -> {
                       log.error("处理MCP消息失败 | sessionId:{}", sessionId, e);
                       return Mono.just(ResponseEntity
                               .status(500)
                               .body(Map.of("error", "Message processing failed")));
                   });
    }
}