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
import reactor.core.publisher.Sinks;

import java.time.Duration;

/**
 * MCP会话末端处理器
 * 核心职责：实现SSE（Server-Sent Events）长连接消息推送的最终处理逻辑，
 * 负责维护SSE流的生命周期、心跳保活、会话资源清理
 *
 * @author cyh
 * @date 2026/03/19
 */
@Slf4j
@Service
public class EndNode extends AbstractMcpSessionSupport {

    /**
     * 核心处理方法：构建并返回SSE消息流，实现消息推送+心跳保活+资源自动清理
     * 重写自AbstractMcpSessionSupport的模板方法，是SSE会话的核心执行逻辑
     *
     * @param requestParameter 前端传入的请求参数（如会话初始化参数）
     * @param context          动态上下文对象，包含当前会话的配置、Sink等核心资源
     * @return Flux<ServerSentEvent < String>> SSE消息流，前端通过该流接收实时消息
     */
    @Override
    protected Flux<ServerSentEvent<String>> doApply(String requestParameter,
                                                    DefaultMcpSessionFactory.DynamicContext context) {
        // 1. 从上下文获取当前会话核心配置与消息发送器
        SessionConfigVO session = context.getSessionConfigVO(); // 会话配置VO（包含会话ID、Sink等）
        String sessionId = session.getSessionId(); // 唯一会话标识，用于日志追踪和资源管理

        log.info("EndNode接管SSE流处理，会话ID: {}", sessionId);

        // 2. 使用Flux.usingWhen实现SSE流的生命周期管理（资源获取-使用-释放）
        // 核心特性：无论正常结束/异常/取消，都会自动清理会话资源，避免内存泄漏
        return Flux.usingWhen(
                // 1. 资源获取：包装会话对象为Mono（响应式单值容器）
                // 作用：1) 适配Reactor异步资源管理规范 2) 实现"懒加载"（前端订阅时才初始化）3) 统一异常处理
                Mono.just(session),

                // 2. 资源使用：基于会话资源构建SSE长连接消息流（核心业务逻辑）
                // 入参s：参数1中Mono生成的SessionConfigVO会话对象（包含Sink和会话ID）
                s -> s
                        // 从会话中获取预创建的Sink，转换为Flux流（Sink是"事件生产者"，Flux是"事件传输管道"）
                        // 作用：Sink中已缓存endpoint等业务事件，转换为Flux后可推送给前端
                        .getSink()
                        .asFlux()
                        // 合并心跳保活流：解决SSE长连接被网关/浏览器超时断开的问题
                        .mergeWith(
                                // 每30秒生成一个ping心跳事件（响应式定时任务，非阻塞）
                                Flux
                                        .interval(Duration.ofSeconds(30))
                                        .map(i -> ServerSentEvent
                                                .<String>builder()
                                                .event("ping") // 前端可识别的心跳事件类型
                                                .data("ping")  // 心跳数据（仅用于保活，无业务意义）
                                                .build())),

                // 3. 正常释放：SSE流正常完成（如服务端主动关闭）时清理会话资源
                s -> sessionManagementService.removeSession(s.getSessionId()),

                // 4. 异常释放：SSE流执行异常（如Sink推送失败、业务逻辑报错）时强制清理资源
                (s, e) -> sessionManagementService.removeSession(s.getSessionId()),

                // 5. 取消释放：捕获前端主动断开连接的"取消信号"（最常用的清理场景）
                // 作用：及时清理无效会话，避免大量僵尸连接占用内存
                s -> sessionManagementService.removeSession(s.getSessionId()));
    }

    /**
     * 获取当前处理器对应的策略处理器
     * 策略模式适配：返回默认策略处理器，用于统一的策略执行入口
     *
     * @param requestParameter 前端请求参数
     * @param context          动态上下文对象
     * @return 默认策略处理器实例
     */
    @Override
    public StrategyHandler<String, DefaultMcpSessionFactory.DynamicContext, Flux<ServerSentEvent<String>>> get(String requestParameter, DefaultMcpSessionFactory.DynamicContext context) {
        return defaultStrategyHandler;
    }
}