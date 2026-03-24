package com.c.domain.session.service.management;

import com.c.domain.session.adapter.repository.McpSessionRepository;
import com.c.domain.session.model.entity.McpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;

/**
 * 会话管理核心服务：只管理会话实体生命周期，不持有 SSE Sink
 *
 * @author cyh
 * @date 2026/03/24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionManagementService implements com.c.domain.session.service.SessionManagementService, InitializingBean, DisposableBean {

    /** 最大会话数量 */
    private static final int MAX_SESSIONS = 10000;
    /** 会话清理周期（分钟） */
    private static final long SESSION_CLEANUP_PERIOD_MINUTES = 5;

    /** 会话仓储 */
    private final McpSessionRepository sessionRepository;
    /** 任务清理器 */
    private final Disposable.Composite disposables = Disposables.composite();

    /**
     * 创建会话（默认超时）
     *
     * @param gatewayId 网关标识
     * @return 会话实体
     */
    @Override
    public Mono<McpSession> createSession(String gatewayId) {
        return createSession(gatewayId, null);
    }

    /**
     * 创建会话（指定超时）
     *
     * @param gatewayId      网关标识
     * @param timeoutSeconds 超时时间
     * @return 会话实体
     */
    @Override
    public Mono<McpSession> createSession(String gatewayId, Integer timeoutSeconds) {
        return Mono.fromCallable(() -> {
                       // 校验会话数量是否达到上限
                       if (sessionRepository.count() >= MAX_SESSIONS) {
                           throw new IllegalStateException("会话数已达上限");
                       }

                       // 生成唯一会话ID
                       String sessionId = java.util.UUID
                               .randomUUID()
                               .toString()
                               .replace("-", "");
                       // 创建会话对象
                       McpSession session = new McpSession(sessionId, gatewayId, timeoutSeconds);
                       // 保存到仓储
                       sessionRepository.save(session);

                       log.info("创建会话 | sessionId={} | gatewayId={} | timeoutSeconds={}",
                               sessionId, gatewayId, session.getTimeoutSeconds());

                       return session;
                   })
                   // 阻塞操作放到弹性线程池
                   .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取会话并刷新活跃时间
     *
     * @param sessionId 会话标识
     * @return 会话实体
     */
    @Override
    public Mono<McpSession> getSession(String sessionId) {
        return Mono
                .justOrEmpty(sessionId)
                .map(sessionRepository::find)
                .filter(Objects::nonNull)
                .flatMap(session -> {
                    // 会话失效或过期则移除
                    if (!session.isActive() || session.isExpired()) {
                        return removeSession(session.getSessionId()).then(Mono.empty());
                    }
                    // 刷新最后访问时间
                    session.touch();
                    return Mono.just(session);
                });
    }

    /**
     * 移除会话
     *
     * @param sessionId 会话标识
     * @return 执行结果
     */
    @Override
    public Mono<Void> removeSession(String sessionId) {
        return Mono
                .fromRunnable(() -> {
                    McpSession session = sessionRepository.find(sessionId);
                    if (session != null) {
                        // 标记失效并从仓储移除
                        session.deactivate();
                        sessionRepository.remove(sessionId);
                        log.info("会话移除 | sessionId={}", sessionId);
                    }
                })
                .then();
    }

    /**
     * 清理过期/失效会话
     *
     * @return 执行结果
     */
    @Override
    public Mono<Void> cleanupExpiredSessions() {
        return Mono
                .fromRunnable(() -> {
                    // 清理前总数
                    int before = sessionRepository
                            .findAll()
                            .size();
                    // 快照防止遍历过程中集合变化
                    var snapshot = new ArrayList<>(sessionRepository.findAll());

                    // 遍历清理失效会话
                    for (McpSession session : snapshot) {
                        if (session == null) continue;
                        if (!session.isActive() || session.isExpired()) {
                            sessionRepository.remove(session.getSessionId());
                        }
                    }

                    // 清理后统计
                    int after = sessionRepository
                            .findAll()
                            .size();
                    int cleaned = before - after;
                    if (cleaned > 0) {
                        log.info("会话 GC 清理完成 | cleaned={} | remaining={}", cleaned, after);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * 关闭服务，清理所有会话
     *
     * @return 执行结果
     */
    @Override
    public Mono<Void> shutdown() {
        return Mono
                .fromRunnable(() -> {
                    log.info("系统关闭，开始清理所有会话...");
                    var snapshot = new ArrayList<>(sessionRepository.findAll());
                    for (McpSession session : snapshot) {
                        sessionRepository.remove(session.getSessionId());
                    }
                })
                .then();
    }

    /**
     * Bean初始化后启动定时清理任务
     */
    @Override
    public void afterPropertiesSet() {
        // 定时任务：定期清理过期会话
        Disposable task = Flux
                .interval(Duration.ofMinutes(SESSION_CLEANUP_PERIOD_MINUTES))
                .onBackpressureDrop()
                .flatMap(i -> cleanupExpiredSessions().onErrorResume(e -> {
                    log.error("会话 GC 清理异常", e);
                    return Mono.empty();
                }))
                .subscribe();

        // 加入任务管理器
        disposables.add(task);
        log.info("Session 管理服务启动完成");
    }

    /**
     * Bean销毁时释放资源
     */
    @Override
    public void destroy() {
        log.info("容器销毁，开始释放 Session 服务资源...");
        // 取消定时任务
        disposables.dispose();
        try {
            // 阻塞关闭，确保资源释放
            shutdown().block(Duration.ofSeconds(10));
        } catch (Exception e) {
            log.error("Session 服务关闭异常", e);
        }
        log.info("Session 服务资源释放完成");
    }
}