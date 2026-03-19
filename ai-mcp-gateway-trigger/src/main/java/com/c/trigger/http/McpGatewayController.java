package com.c.trigger.http;

import com.c.api.IMcpGatewayService;
import com.c.cases.mcp.api.service.IMcpSessionService;
import com.c.types.enums.ResponseCode;
import com.c.types.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * MCP 网关服务接口管理
 *
 * @author cyh
 * @date 2026/03/18
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT
        , RequestMethod.DELETE, RequestMethod.OPTIONS})
@RequestMapping("/api/v1/mcp")
public class McpGatewayController implements IMcpGatewayService {

    /** MCP 会话业务服务 */
    private final IMcpSessionService mcpSessionService;

    /**
     * 建立 SSE 连接并创建 MCP 会话
     * 测试地址: curl -N http://localhost:8091/api-gateway/api/v1/mcp/test10001/sse
     *
     * @param gatewayId 网关唯一标识 ID
     * @return ServerSentEvent 响应流
     */
    @Override
    @GetMapping(value = "{gatewayId}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> establishSSEConnection(@PathVariable("gatewayId") String gatewayId) {
        return Mono.justOrEmpty(gatewayId)
                   // 校验 ID：确保不为 null 且不是空字符串或空格
                   .filter(StringUtils::isNotBlank)
                   // 异常处理：参数非法时返回预定义的错误码
                   .switchIfEmpty(Mono.error(() -> new AppException(ResponseCode.ILLEGAL_PARAMETER)))
                   // 日志埋点：记录接入请求，便于链路追踪
                   .doOnNext(id -> log.info("准备建立 MCP SSE 连接 | 网关ID: {}", id))
                   // 业务分发：将单个信号转换为多个 SSE 事件流
                   .flatMapMany(mcpSessionService::createMcpSession)
                   // 异常监控：记录流建立过程中发生的非预期错误
                   .doOnError(e -> log.error("建立 MCP SSE 连接时发生异常 | 网关ID: {}", gatewayId, e))
                   // 状态记录：记录流最终结束、断开或取消的状态
                   .doOnTerminate(() -> log.debug("MCP SSE 连接进程结束 | 网关ID: {}", gatewayId));
    }
}