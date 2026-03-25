package com.c.domain.session.service.management;

import com.c.domain.session.adapter.repository.SessionRepository;
import com.c.domain.session.model.entity.McpSession;
import com.c.domain.session.service.SessionManagementService;
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
import java.util.UUID;

/**
 * MCP 会话管理核心服务实现
 * 负责会话全生命周期管理，包括创建、获取、自动续期、主动销毁、过期清理及后台任务管理
 * 基于数据库实现持久化，支持集群部署与服务重启恢复
 *
 * @author cyh
 * @date 2026/03/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionManagementServiceImpl implements SessionManagementService, InitializingBean, DisposableBean {

    /** 系统允许的最大活跃会话数量，用于限流保护 */
    private static final int MAX_SESSIONS = 10000;

    /** 过期会话定时清理任务的执行周期，单位：分钟 */
    private static final long SESSION_CLEANUP_PERIOD_MINUTES = 5;

    /** 定时任务组合器，统一管理后台定时任务生命周期 */
    private final Disposable.Composite disposables = Disposables.composite();

    /** 会话持久化仓储，用于数据操作 */
    private final SessionRepository sessionRepository;

    /**
     * 使用默认超时时间创建新会话
     *
     * @param gatewayId 网关唯一标识
     * @return 包含新会话的Mono对象
     */
    @Override
    public Mono<McpSession> createSession(String gatewayId) {
        return createSession(gatewayId, null);
    }

    /**
     * 创建新会话，支持指定超时时间
     * 会校验会话上限、生成唯一标识、初始化会话并持久化到数据库
     *
     * @param gatewayId      网关唯一标识
     * @param timeoutSeconds 会话超时时间，为空则使用默认值
     * @return 包含新会话的Mono对象
     */
    @Override
    public Mono<McpSession> createSession(String gatewayId, Integer timeoutSeconds) {
        return Mono.fromCallable(() -> {
                       // 查询当前活跃会话数，判断是否达到系统上限
                       long total = sessionRepository.countActiveSessions();
                       if (total >= MAX_SESSIONS) {
                           throw new IllegalStateException("会话数量已达系统上限");
                       }

                       // 生成无横杠的UUID作为全局唯一会话ID
                       String sessionId = UUID
                               .randomUUID()
                               .toString()
                               .replace("-", "");
                       // 构建会话实体并初始化最后访问时间
                       McpSession session = new McpSession(sessionId, gatewayId, timeoutSeconds);
                       session.touch();
                       // 将会话持久化到数据库
                       sessionRepository.save(session);

                       log.info("创建会话 | sessionId={} | gatewayId={}", sessionId, gatewayId);
                       return session;
                   })
                   // 数据库阻塞操作放入弹性线程池执行，避免阻塞反应式流
                   .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取会话并自动完成续期
     * 会话不存在/已过期/已失效时会自动清理并返回空
     *
     * @param sessionId 会话唯一标识
     * @return 有效会话的Mono对象
     */
    @Override
    public Mono<McpSession> getSession(String sessionId) {
        return Mono.justOrEmpty(sessionId)
                   // 根据ID查询会话
                   .map(sessionRepository::findBySessionId)
                   .flatMap(session -> {
                       // 会话不存在直接返回空
                       if (session == null) return Mono.empty();
                       // 会话已失效或过期，执行清理并返回空
                       if (!session.isActive() || session.isExpired()) {
                           return removeSession(sessionId).then(Mono.empty());
                       }
                       // 会话有效，刷新访问时间并更新数据库
                       session.touch();
                       sessionRepository.update(session);
                       return Mono.just(session);
                   });
    }

    /**
     * 主动销毁指定会话，标记失效并从数据库删除
     *
     * @param sessionId 会话唯一标识
     * @return 执行完成的Mono信号
     */
    @Override
    public Mono<Void> removeSession(String sessionId) {
        return Mono
                .fromRunnable(() -> {
                    McpSession session = sessionRepository.findBySessionId(sessionId);
                    if (session != null) {
                        // 将会话标记为失效状态
                        session.deactivate();
                        // 从数据库物理删除会话记录
                        sessionRepository.deleteById(sessionId);
                        log.info("会话已移除 | sessionId={}", sessionId);
                    }
                })
                .then()
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 主动触发过期会话清理操作
     * 清理数据库中所有过期或已失效的会话
     *
     * @return 执行完成的Mono信号
     */
    @Override
    public Mono<Void> cleanupExpiredSessions() {
        return Mono
                .fromRunnable(() -> {
                    // 调用仓储层执行过期会话删除
                    int cleaned = sessionRepository.deleteExpiredSessions();
                    if (cleaned > 0) {
                        log.info("过期会话清理完成 | 清理数量={}", cleaned);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * 优雅关闭会话管理服务
     * 停止所有后台定时任务，释放系统资源
     *
     * @return 关闭完成的Mono信号
     */
    @Override
    public Mono<Void> shutdown() {
        return Mono
                .fromRunnable(disposables::dispose)
                .then();
    }

    /**
     * Bean初始化完成后执行，启动后台定时清理任务
     */
    @Override
    public void afterPropertiesSet() {
        // 创建定时任务：定期清理过期会话
        Disposable task = Flux
                .interval(Duration.ofMinutes(SESSION_CLEANUP_PERIOD_MINUTES))
                .flatMap(t -> cleanupExpiredSessions().onErrorResume(e -> {
                    log.error("定时清理会话异常", e);
                    return Mono.empty();
                }))
                .subscribe();
        // 将任务加入组合器统一管理
        disposables.add(task);
        log.info("MCP 会话管理服务启动完成");
    }

    /**
     * Bean销毁时执行，释放所有定时任务资源，保证优雅停机
     */
    @Override
    public void destroy() {
        disposables.dispose();
        log.info("会话管理服务已安全关闭");
    }
}