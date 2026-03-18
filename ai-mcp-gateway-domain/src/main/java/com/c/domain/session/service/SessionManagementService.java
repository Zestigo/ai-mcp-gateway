package com.c.domain.session.service;

import com.c.domain.session.model.valobj.SessionConfigVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SessionManagementService implements ISessionManagementService, InitializingBean, DisposableBean {

    /** 会话静默超时阈值：30分钟未更新访问时间则判定为过期 */
    private static final long SESSION_TIMEOUT_MINUTES = 30;

    /**
     * 活跃会话容器
     * 使用 ConcurrentHashMap 确保在多线程（Netty 线程与清理线程）并发读写下的可见性与原子性
     */
    private final Map<String, SessionConfigVO> activeSessions = new ConcurrentHashMap<>();

    // ==========================================
    // 1. 会话核心操作 (创建 / 获取 / 注销)
    // ==========================================

    /**
     * 创建一个新的响应式会话
     * fromCallable 将同步构建逻辑包装为异步序列，确保在 WebFlux 调用链中平滑编排
     */
    @Override
    public Mono<SessionConfigVO> createSession(String gatewayId) {
        return Mono.fromCallable(() -> {
            // [1] 生成全局唯一会话标识
            String sessionId = UUID
                    .randomUUID()
                    .toString();

            // [2] 初始化 Sinks.Many (多播模式)
            // onBackpressureBuffer: 当推送速度超过网络下发速度时，在内存中缓冲消息
            Sinks.Many<ServerSentEvent<String>> sink = Sinks
                    .many()
                    .multicast()
                    .onBackpressureBuffer();

            // [3] 构建领域值对象并存入并发容器
            SessionConfigVO vo = new SessionConfigVO(sessionId, sink);
            activeSessions.put(sessionId, vo);

            // [4] 计算端点地址，并通过 Sink 推送引导信号给前端
            String endpoint = String.format("/%s/mcp/message?sessionId=%s", gatewayId, sessionId);
            emitEndpointSignal(sink, endpoint);

            log.info("会话就绪 [Gateway: {}, Session: {}], 当前在线连接数: {}", gatewayId, sessionId, activeSessions.size());
            return vo;
        });
    }

    /**
     * 获取指定 ID 的活跃会话
     * 包含状态校验与访问时间自动续期（滑动窗口机制）
     */
    @Override
    public Mono<SessionConfigVO> getSession(String sessionId) {
        return Mono
                .justOrEmpty(sessionId)
                .mapNotNull(activeSessions::get)
                .filter(vo -> {
                    // 双重校验：volatile 保证的 active 状态 + 基于 Instant 时间轴的过期判定
                    if (vo.isActive() && !vo.isExpired(SESSION_TIMEOUT_MINUTES)) {
                        vo.updateLastAccessed(); // 命中后刷新访问时间，维持长连接生命力
                        return true;
                    }
                    return false;
                });
    }

    /**
     * 显式注销会话并切断网络流
     * 只有当被 subscribe() 时，这一系列具有副作用（Side Effect）的释放操作才会执行
     */
    @Override
    public Mono<Void> removeSession(String sessionId) {
        return Mono.fromRunnable(() -> {
            // [1] 逻辑断开：从并发容器剔除，确保新请求无法再通过 sessionId 找到该会话
            SessionConfigVO vo = activeSessions.remove(sessionId);
            if (vo == null) return;

            // [2] 状态标记：将 volatile 修饰的 active 置为 false
            // 确保其他持有旧引用的线程（如消息推送任务）能立即感知失效，停止无效计算
            vo.markInactive();

            // [3] 物理切断：驱动底层 HTTP/TCP 连接发送 EOF 报文
            // 这是关键步骤！若不执行，客户端将持续 Pending，服务器 Socket 句柄会发生泄露
            Sinks.EmitResult result = vo
                    .getSink()
                    .tryEmitComplete();

            // [4] 结果校验：监控信号发射状态，辅助排查潜在的流异常
            if (result.isFailure()) {
                log.warn("会话管道释放失败 [ID: {}], 状态码: {}", sessionId, result);
            }

            log.info("会话已安全注销 -> ID: {}, 剩余活跃数: {}", sessionId, activeSessions.size());
        });
    }

    // ==========================================
    // 2. 信号发射辅助
    // ==========================================

    /**
     * 发送 SSE 端点指令
     * 相比 tryEmitNext，emitNext 允许通过 Handler 声明式处理推送失败的边界场景
     */
    private void emitEndpointSignal(Sinks.Many<ServerSentEvent<String>> sink, String data) {
        sink.emitNext(ServerSentEvent
                        .<String>builder()
                        .event("endpoint") // 明确事件类型，便于前端使用 addEventListener 精准监听
                        .data(data)        // 负载：客户端后续通信的物理地址
                        .build(),

                // 核心并发策略：FAIL_FAST
                // Reactor Sinks 要求序列化访问。若检测到其他线程（如心跳任务）正在占用 Sink，
                // FAIL_FAST 会立即返回失败以保护当前线程（如 EventLoop），防止挂起或陷入重试循环
                Sinks.EmitFailureHandler.FAIL_FAST);
    }

    // ==========================================
    // 3. 自动清理与生命周期钩子
    // ==========================================

    /**
     * 自动清理过期的不活跃会话
     */
    @Override
    public Mono<Void> cleanupExpiredSessions() {
        return Mono.fromRunnable(() -> {
            int beforeSize = activeSessions.size();
            // 使用迭代器安全的 removeIf 批量过滤
            activeSessions
                    .values()
                    .removeIf(vo -> {
                        // 判定标准：主动注销标志位 或 超过 30 分钟未交互
                        boolean isDead = !vo.isActive() || vo.isExpired(SESSION_TIMEOUT_MINUTES);
                        if (isDead) {
                            vo
                                    .getSink()
                                    .tryEmitComplete(); // 内存移除前必须终结管道，防止句柄泄露
                        }
                        return isDead;
                    });

            int cleaned = beforeSize - activeSessions.size();
            if (cleaned > 0) {
                log.info("会话回收引擎 [GC]: 清理过期连接数: {}, 存活数: {}", cleaned, activeSessions.size());
            }
        });
    }

    /**
     * InitializingBean 实现：启动常驻后台的异步清理时钟
     */
    @Override
    public void afterPropertiesSet() {
        // 创建心跳时钟流，每 5 分钟触发一次清理任务
        reactor.core.publisher.Flux
                .interval(Duration.ofMinutes(5))
                .flatMap(tick -> cleanupExpiredSessions())
                .subscribe(unused -> {
                        }, e -> log.error("会话自动清理引擎发生非预期异常: ", e) // 捕获异常确保流不因单次错误终止
                );
        log.info("会话管理服务启动成功 -> 超时阈值: {} min, 自动清理周期: 5 min", SESSION_TIMEOUT_MINUTES);
    }

    /**
     * 系统级关机清理
     */
    @Override
    public Mono<Void> shutdown() {
        return Mono
                .fromRunnable(() -> {
                    // 迭代所有 Key 调用标准移除逻辑，确保日志和信号的完整性
                    activeSessions
                            .keySet()
                            .forEach(this::removeSession);
                    activeSessions.clear();
                })
                .then();
    }

    /**
     * DisposableBean 实现：确保 JVM 停止前全量回收网络资源
     */
    @Override
    public void destroy() {
        log.info("检测到容器销毁信号，执行全量资源回收...");
        try {
            // block() 在销毁阶段强制同步等待，确保清理任务真正完成后再退出进程
            this
                    .shutdown()
                    .block(Duration.ofSeconds(10));
            log.info("全量资源回收完成 [Graceful Shutdown]");
        } catch (Exception e) {
            log.error("系统停机回收超时或失败: ", e);
        }
    }
}