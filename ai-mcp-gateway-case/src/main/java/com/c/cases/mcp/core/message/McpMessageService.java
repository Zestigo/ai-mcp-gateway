package com.c.cases.mcp.core.message;

import com.c.cases.mcp.api.model.McpSessionRequest;
import com.c.cases.mcp.api.service.IMcpSessionService;
import com.c.cases.mcp.core.session.factory.DefaultMcpSessionFactory;
import com.c.cases.mcp.core.session.factory.IMcpSessionFactory;
import com.c.domain.session.model.valobj.McpSchemaVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class McpMessageService implements IMcpSessionService {

    @Resource
    private IMcpSessionFactory mcpSessionFactory;

    private final Map<String, Sinks.Many<ServerSentEvent<String>>> sessionPool = new ConcurrentHashMap<>();

    @Override
    public Flux<ServerSentEvent<String>> createMcpSession(String gatewayId) {
        return Flux.defer(() -> {
            Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().multicast().directBestEffort();
            sessionPool.put(gatewayId, sink);
            log.info("MCP初始会话注册 | gatewayId: {}", gatewayId);

            try {
                return mcpSessionFactory.strategyHandler(gatewayId)
                                        .apply(gatewayId, new DefaultMcpSessionFactory.DynamicContext())
                                        .doOnNext(sse -> {
                                            String sessionId = extractSessionId(sse.data());
                                            if (sessionId != null) {
                                                sessionPool.put(sessionId, sink);
                                                log.info("自动绑定 UUID 到会话池 | sessionId: {}", sessionId);
                                            }
                                        })
                                        .mergeWith(sink.asFlux())
                                        .doOnTerminate(() -> cleanUp(sink, gatewayId))
                                        .doOnCancel(() -> cleanUp(sink, gatewayId));
            } catch (Exception e) {
                log.error("创建会话流失败", e);
                return Flux.error(e);
            }
        });
    }

    private String extractSessionId(String data) {
        if (data == null) return null;
        try {
            String[] parts = data.split("/sessions/");
            if (parts.length > 1) return parts[1].split("/")[0];
        } catch (Exception e) { log.warn("提取ID失败: {}", data); }
        return null;
    }

    private void cleanUp(Sinks.Many<ServerSentEvent<String>> sink, String gatewayId) {
        sessionPool.values().removeIf(s -> s.equals(sink));
        log.info("会话池清理完成 | gatewayId: {}", gatewayId);
    }

    @Override
    public void pushMessage(String sessionId, Object message) {
        Sinks.Many<ServerSentEvent<String>> sink = sessionPool.get(sessionId);
        if (sink == null) {
            log.warn("推送失败：未找到活跃会话 | sessionId: {}", sessionId);
            return;
        }

        try {
            // 【核心修正】判定类型：如果是 String 说明 controller 已经转过 JSON 了，直接发原始内容
            String data = (message instanceof String s) ? s : McpSchemaVO.toJson(message);

            // 强制通过 ServerSentEvent<String> 传输，Spring 发现是 String 就不会再乱加引号
            sink.tryEmitNext(ServerSentEvent.<String>builder()
                                            .data(data)
                                            .event("message")
                                            .build());
            log.info("消息推送成功 | sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("推送异常 | sessionId: {}", sessionId, e);
        }
    }

    @Override
    public Flux<ServerSentEvent<String>> createMcpSession(McpSessionRequest request) {
        return createMcpSession(request.getGatewayId());
    }
}