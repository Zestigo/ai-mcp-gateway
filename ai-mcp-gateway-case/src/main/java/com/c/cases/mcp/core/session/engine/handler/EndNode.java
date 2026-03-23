package com.c.cases.mcp.core.session.engine.handler;

import com.c.cases.mcp.core.session.factory.DefaultMcpSessionFactory;
import com.c.cases.mcp.core.message.strategy.StrategyHandler;
import com.c.cases.mcp.support.AbstractMcpSessionSupport;
import com.c.domain.session.model.valobj.SessionConfigVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * MCP会话末端处理器：维护SSE长连接生命周期、心跳保活、资源清理
 *
 * @author cyh
 * @date 2026/03/19
 */
@Slf4j
@Service
public class EndNode extends AbstractMcpSessionSupport {

    /**
     * 构建SSE消息流，实现消息推送+心跳保活+资源自动清理
     *
     * @param requestParameter 前端请求参数
     * @param context          会话动态上下文
     * @return SSE事件流
     */
    @Override
    protected Flux<ServerSentEvent<String>> doApply(String requestParameter,
                                                    DefaultMcpSessionFactory.DynamicContext context) {
        // 获取会话配置与唯一标识
        SessionConfigVO session = context.getSessionConfigVO();
        String sessionId = session.getSessionId();
        log.info("EndNode接管SSE流 | sessionId: {}", sessionId);

        // Flux.usingWhen：统一管理会话资源生命周期（获取-使用-释放）
        return Flux.usingWhen(
                // 资源获取：包装会话对象为Mono，适配响应式规范
                Mono.just(session),
                // 资源使用：合并业务流+心跳流，实现长连接保活
                s -> s
                        .getSink()
                        .asFlux()
                        .mergeWith(Flux
                                .interval(Duration.ofSeconds(30))
                                .map(i -> ServerSentEvent
                                        .<String>builder()
                                        .comment("ping")
                                        .build())),
                // 资源释放：正常/异常/取消场景统一清理会话
                s -> sessionManagementService.removeSession(s.getSessionId()),
                (s, e) -> sessionManagementService.removeSession(s.getSessionId()),
                s -> sessionManagementService.removeSession(s.getSessionId()));
    }

    /**
     * 获取默认策略处理器
     *
     * @param requestParameter 前端请求参数
     * @param context          会话动态上下文
     * @return 默认策略处理器实例
     */
    @Override
    public StrategyHandler<String, DefaultMcpSessionFactory.DynamicContext, Flux<ServerSentEvent<String>>> get(String requestParameter, DefaultMcpSessionFactory.DynamicContext context) {
        return defaultStrategyHandler;
    }
}