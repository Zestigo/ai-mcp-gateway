package com.c.cases.mcp.session;

import com.c.cases.mcp.api.model.McpSessionRequest;
import com.c.cases.mcp.api.service.McpSessionService;
import com.c.domain.session.adapter.repository.McpSessionRepository;
import com.c.domain.session.adapter.repository.SessionSsePort;
import com.c.domain.session.model.entity.McpSession;
import com.c.domain.session.model.valobj.McpSchemaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * MCP会话运行时服务
 * 实现会话创建、SSE连接管理、消息推送、心跳保活、资源清理等核心能力
 *
 * @author cyh
 * @date 2026/03/24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpSessionServiceImpl implements McpSessionService {

    /** 会话仓储 */
    private final McpSessionRepository mcpSessionRepository;

    /** SSE通道适配器 */
    private final SessionSsePort sessionSsePort;

    /** 服务完整地址 */
    @Value("${service.full-url}")
    private String serviceFullUrl;

    /** 应用上下文路径 */
    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * 根据网关ID快速创建会话
     *
     * @param gatewayId 网关标识
     * @return SSE事件流
     */
    @Override
    public Flux<ServerSentEvent<String>> createMcpSession(String gatewayId) {
        // 构建默认请求对象并调用重载方法
        return createMcpSession(McpSessionRequest
                .builder()
                .gatewayId(gatewayId)
                .build());
    }

    /**
     * 根据完整请求创建会话并建立SSE连接
     *
     * @param request 会话请求对象
     * @return SSE事件流
     */
    @Override
    public Flux<ServerSentEvent<String>> createMcpSession(McpSessionRequest request) {
        // 延迟订阅，保证每次请求生成新会话
        return Flux.defer(() -> {

            // 生成随机唯一会话ID
            String sessionId = UUID
                    .randomUUID()
                    .toString();
            // 从请求获取网关ID
            String gatewayId = request.getGatewayId();
            // 从请求获取超时时间
            Integer timeout = request.getTimeout();

            // 创建会话实体
            McpSession session = new McpSession(sessionId, gatewayId, timeout);
            // 保存会话到仓储
            mcpSessionRepository.save(session);

            // 创建SSE消息推送通道
            Sinks.Many<ServerSentEvent<String>> sink = sessionSsePort.create(sessionId);

            log.info("创建会话 | sessionId={} | gatewayId={} | timeout={}", sessionId, gatewayId, timeout);

            // 构造客户端消息接收端点完整路径
            String endpointPath =
                    serviceFullUrl + contextPath + "/api/v1/gateways/" + gatewayId + "/sessions/" + sessionId +
                            "/messages";

            // 构造endpoint推送事件
            ServerSentEvent<String> endpointEvent = ServerSentEvent
                    .<String>builder()
                    .event("endpoint")
                    .data(endpointPath)
                    .build();

            // 组装SSE流：先推送endpoint，再拼接业务消息+心跳
            return Flux
                    .just(endpointEvent)
                    .concatWith(sink
                            .asFlux()
                            .mergeWith(heartbeat()))
                    // 连接建立日志
                    .doOnSubscribe(sub -> log.info("SSE连接建立 | sessionId={}", sessionId))
                    // 连接关闭后执行资源清理
                    .doFinally(signal -> cleanup(sessionId));
        });
    }

    /**
     * 向指定会话推送消息
     *
     * @param sessionId 会话标识
     * @param message   消息内容
     */
    @Override
    public void pushMessage(String sessionId, Object message) {
        // 获取会话对应的SSE通道
        Sinks.Many<ServerSentEvent<String>> sink = sessionSsePort.get(sessionId);

        // 通道不存在则直接返回
        if (sink == null) {
            log.warn("session不存在 | {}", sessionId);
            return;
        }

        try {
            // 消息序列化：字符串直接使用，对象转JSON
            String data = (message instanceof String s) ? s : McpSchemaVO.toJson(message);

            // 尝试发送消息到客户端
            Sinks.EmitResult result = sink.tryEmitNext(ServerSentEvent
                    .<String>builder()
                    .event("message")
                    .data(data)
                    .build());

            // 发送失败打印警告日志
            if (result.isFailure()) {
                log.warn("发送失败 | sessionId={} | result={}", sessionId, result);
            }

        } catch (Exception e) {
            // 消息发送异常捕获
            log.error("推送异常 | sessionId={}", sessionId, e);
        }
    }

    /**
     * 向网关下所有会话广播消息
     *
     * @param gatewayId 网关标识
     * @param message   消息内容
     */
    public void broadcast(String gatewayId, Object message) {
        // 获取网关下所有会话ID
        Set<String> sessionIds = mcpSessionRepository.findByGateway(gatewayId);

        // 遍历所有会话进行消息推送
        for (String sessionId : sessionIds) {
            pushMessage(sessionId, message);
        }
    }

    /**
     * 构建30秒一次的心跳流，保持SSE连接存活
     *
     * @return 心跳事件流
     */
    private Flux<ServerSentEvent<String>> heartbeat() {
        return Flux
                .interval(Duration.ofSeconds(30))
                .map(i -> ServerSentEvent
                        .<String>builder()
                        .comment("ping")
                        .build());
    }

    /**
     * 会话关闭后资源清理：删除会话、关闭SSE通道
     *
     * @param sessionId 会话标识
     */
    private void cleanup(String sessionId) {
        try {
            // 从仓储删除会话
            mcpSessionRepository.remove(sessionId);
            // 关闭并移除SSE通道
            sessionSsePort.remove(sessionId);
            log.info("释放会话 | sessionId={}", sessionId);
        } catch (Exception e) {
            // 清理异常捕获
            log.error("清理失败 | sessionId={}", sessionId, e);
        }
    }
}