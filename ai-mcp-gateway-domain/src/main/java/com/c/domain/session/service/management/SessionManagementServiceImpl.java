package com.c.domain.session.service.management;

import com.c.domain.session.adapter.repository.SessionRepository;
import com.c.domain.session.model.entity.McpSession;
import com.c.domain.session.service.SessionManagementService;
import com.c.types.utils.InstanceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理服务实现类
 * 负责MCP会话全生命周期管理：创建、查询、续期、销毁、过期清理
 * 维护本地连接管道，支持分布式节点会话路由与优雅停机
 *
 * @author cyh
 * @date 2026/03/27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionManagementServiceImpl implements SessionManagementService, InitializingBean, DisposableBean {

    /** 系统最大会话数量限制，默认10000 */
    @Value("${mcp.session.max-sessions:10000}")
    private int maxSessions;

    /** 过期会话定时清理周期，单位：分钟，默认5分钟 */
    @Value("${mcp.session.cleanup-period-minutes:5}")
    private long cleanupPeriodMinutes;

    /** 会话仓储接口，负责会话数据持久化操作 */
    private final SessionRepository sessionRepository;

    /** 实例信息提供者，用于获取当前节点IP */
    private final InstanceProvider instanceProvider;

    /** 响应式任务组合容器，用于统一管理定时任务与资源释放 */
    private final Disposable.Composite disposables = Disposables.composite();

    /** 本地连接缓存：key-会话ID，value-SSE连接Sink，支持高并发读写 */
    private final Map<String, Sinks.Many<ServerSentEvent<String>>> localSinks = new ConcurrentHashMap<>();

    /**
     * 创建会话（使用默认超时时间）
     *
     * @param gatewayId 网关唯一标识
     * @return 创建完成的会话实体异步流
     */
    @Override
    public Mono<McpSession> createSession(String gatewayId) {
        return createSession(gatewayId, null);
    }

    /**
     * 创建会话（支持自定义超时时间）
     * 校验会话上限 → 生成会话ID → 绑定节点IP → 初始化连接管道 → 持久化会话
     *
     * @param gatewayId      网关唯一标识
     * @param timeoutSeconds 自定义会话超时时间，为空使用默认值
     * @return 创建完成的会话实体异步流
     */
    @Override
    public Mono<McpSession> createSession(String gatewayId, Integer timeoutSeconds) {
        return Mono
                .fromCallable(() -> {
                    // 校验系统会话数量是否达到上限
                    long total = sessionRepository.countActiveSessions();
                    if (total >= maxSessions) {
                        log.warn("[会话创建] 已达到上限: {}", maxSessions);
                        throw new IllegalStateException("会话数量已达上限");
                    }

                    // 获取当前服务节点物理IP，用于分布式会话路由
                    String currentIp = instanceProvider.getHostIp();
                    String sessionId = UUID
                            .randomUUID()
                            .toString();

                    // 构建会话实体，绑定当前节点IP与基础信息
                    McpSession session = McpSession
                            .builder()
                            .sessionId(sessionId)
                            .gatewayId(gatewayId)
                            .hostIp(currentIp)
                            .timeoutSeconds(timeoutSeconds)
                            .active(true)
                            .createTime(Instant.now())
                            .lastAccessTime(Instant.now())
                            .build();

                    log.info("[会话准备] sessionId: {}, gatewayId: {}, hostIp: {}", sessionId, gatewayId, currentIp);

                    // 初始化SSE响应式管道，用于消息推送
                    Sinks.Many<ServerSentEvent<String>> sink = Sinks
                            .many()
                            .multicast()
                            .onBackpressureBuffer();
                    this.registerSink(sessionId, sink);

                    // 将会话信息持久化到数据库
                    sessionRepository.save(session);

                    return session;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 根据会话ID获取会话信息
     * 会话存在性校验 → 过期自动清理 → 自动续期 → 更新持久化
     *
     * @param sessionId 会话唯一标识
     * @return 会话实体异步流，不存在返回空
     */
    @Override
    public Mono<McpSession> getSession(String sessionId) {
        return Mono
                .fromCallable(() -> sessionRepository.findBySessionId(sessionId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(session -> {
                    if (session == null) return Mono.empty();

                    // 会话失效或过期，执行自动清理
                    if (!session.isActive() || session.isExpired()) {
                        log.info("[会话过期] 自动清理 sessionId: {}", sessionId);
                        return removeSession(sessionId).then(Mono.empty());
                    }

                    // 刷新会话访问时间，实现自动续期
                    session.touch();
                    return Mono
                            .fromRunnable(() -> sessionRepository.update(session))
                            .subscribeOn(Schedulers.boundedElastic())
                            .thenReturn(session);
                });
    }

    /**
     * 注销并移除会话
     * 关闭本地连接管道 → 删除持久化数据 → 清理缓存
     *
     * @param sessionId 会话唯一标识
     * @return 移除完成异步信号
     */
    @Override
    public Mono<Void> removeSession(String sessionId) {
        return Mono
                .fromRunnable(() -> {
                    // 清理本地连接并关闭管道
                    Sinks.Many<ServerSentEvent<String>> sink = localSinks.remove(sessionId);
                    if (sink != null) {
                        sink.tryEmitComplete();
                    }
                    // 删除数据库会话记录
                    sessionRepository.deleteById(sessionId);
                    log.info("[会话注销] sessionId: {}", sessionId);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * 注册会话本地连接管道
     *
     * @param sessionId 会话唯一标识
     * @param sink      SSE响应式消息管道
     */
    @Override
    public void registerSink(String sessionId, Sinks.Many<ServerSentEvent<String>> sink) {
        if (sessionId != null && sink != null) {
            localSinks.put(sessionId, sink);
        }
    }

    /**
     * 获取本地会话连接管道
     *
     * @param sessionId 会话唯一标识
     * @return 本地连接管道Optional包装对象
     */
    @Override
    public Optional<Sinks.Many<ServerSentEvent<String>>> getLocalSink(String sessionId) {
        return Optional.ofNullable(localSinks.get(sessionId));
    }

    /**
     * 获取所有本地会话ID集合
     *
     * @return 本地会话ID集合
     */
    @Override
    public Set<String> getAllLocalKeys() {
        return localSinks.keySet();
    }

    /**
     * 主动清理数据库中过期会话
     *
     * @return 清理完成异步信号
     */
    @Override
    public Mono<Void> cleanupExpiredSessions() {
        return Mono
                .fromRunnable(() -> {
                    int cleaned = sessionRepository.deleteExpiredSessions();
                    if (cleaned > 0) log.info("[自动清理] 清理过期记录: {}", cleaned);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * Bean初始化完成后执行
     * 启动定时清理任务，注册任务到资源容器
     */
    @Override
    public void afterPropertiesSet() {
        // 创建定时清理任务，周期执行过期会话清理
        Disposable task = Flux
                .interval(Duration.ofMinutes(cleanupPeriodMinutes))
                .flatMap(t -> cleanupExpiredSessions())
                .doOnError(e -> log.error("[任务异常] 清理任务失败", e))
                .retry()
                .subscribe();
        disposables.add(task);
        log.info("[服务就绪] 会话管理启动成功 | 节点IP: {}", instanceProvider.getHostIp());
    }

    /**
     * Bean销毁方法
     * 调用优雅停机逻辑，阻塞等待资源释放完成
     */
    @Override
    public void destroy() {
        this
                .shutdown()
                .block(Duration.ofSeconds(5));
    }

    /**
     * 优雅停机，释放所有资源
     * 关闭定时任务 → 关闭所有连接管道 → 清空本地缓存
     *
     * @return 停机完成异步信号
     */
    @Override
    public Mono<Void> shutdown() {
        return Mono
                .fromRunnable(() -> {
                    // 释放所有响应式任务资源
                    disposables.dispose();
                    // 关闭所有本地SSE连接
                    localSinks
                            .values()
                            .forEach(Sinks.Many::tryEmitComplete);
                    // 清空本地连接缓存
                    localSinks.clear();
                    log.info("[优雅停机] 会话资源已安全释放");
                })
                .then();
    }
}