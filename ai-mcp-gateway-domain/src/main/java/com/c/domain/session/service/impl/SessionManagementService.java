package com.c.domain.session.service.impl;

import com.c.domain.session.model.valobj.SessionConfigVO;
import com.c.domain.session.service.ISessionManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理核心服务：负责SSE会话的创建/查询/删除/过期清理，基于Reactor实现响应式资源管理
 *
 * @author cyh
 * @date 2026/03/19
 */
@Slf4j
@Service
public class SessionManagementService implements ISessionManagementService, InitializingBean, DisposableBean {

    /** 会话超时时间（分钟）：30分钟无活动则判定为过期 */
    private static final long SESSION_TIMEOUT_MINUTES = 30;
    /** 最大会话数上限：防止系统资源耗尽 */
    private static final int MAX_SESSIONS = 10000;

    /** 活跃会话存储：ConcurrentHashMap保证多线程安全 */
    private final Map<String, SessionConfigVO> activeSessions = new ConcurrentHashMap<>();
    /** 响应式资源容器：统一管理定时任务等可销毁资源 */
    private final Disposable.Composite disposables = Disposables.composite();

    // ==========================================
    // 1. API层（对外语义）
    // ==========================================

    /**
     * 创建SSE会话（响应式）
     *
     * @param gatewayId 网关标识，用于拼接消息推送端点
     * @return Mono<SessionConfigVO> 会话配置VO（含sessionId、Sink）
     */
    @Override
    public Mono<SessionConfigVO> createSession(String gatewayId) {
        return Mono
                // 把同步创建逻辑包装为Mono，避免阻塞响应式主线程
                .fromCallable(() -> {
                    // 会话数限流：达到上限时抛出异常
                    if (activeSessions.size() >= MAX_SESSIONS) {
                        throw new IllegalStateException("会话数已达上限");
                    }

                    // 生成无横线的UUID作为会话唯一标识
                    String sessionId = UUID
                            .randomUUID()
                            .toString()
                            .replace("-", "");

                    // 创建单播Sink：用于向SSE流推送消息，背压策略为缓冲区
                    Sinks.Many<ServerSentEvent<String>> sink = Sinks
                            .many()
                            .unicast()
                            .onBackpressureBuffer();

                    // 封装会话配置并加入活跃会话池
                    SessionConfigVO vo = new SessionConfigVO(sessionId, sink);
                    activeSessions.put(sessionId, vo);

                    // 推送端点事件：告知前端消息接收地址
                    emit(vo.getSink(), "endpoint", "/" + gatewayId + "/mcp/message?sessionId=" + sessionId);

                    log.info("创建会话 sessionId={}, 当前会话数={}", sessionId, activeSessions.size());

                    return vo;
                })
                // 指定执行线程池：boundedElastic适配阻塞/IO型操作
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 查询有效会话（响应式）
     *
     * @param sessionId 会话唯一标识
     * @return Mono<SessionConfigVO> 有效会话VO（无则返回空Mono）
     */
    @Override
    public Mono<SessionConfigVO> getSession(String sessionId) {
        return Mono
                // 空值校验：避免NullPointerException
                .justOrEmpty(sessionId)
                // 从活跃会话池查询
                .map(activeSessions::get)
                // 过滤空值
                .filter(Objects::nonNull)
                // 过滤无效会话（非活跃/已过期）
                .filter(this::isSessionValid)
                // 更新最后访问时间：重置会话过期计时
                .doOnNext(SessionConfigVO::updateLastAccessed);
    }

    /**
     * 手动移除会话（响应式）
     *
     * @param sessionId 会话唯一标识
     * @return Mono<Void> 空响应，标识操作完成
     */
    @Override
    public Mono<Void> removeSession(String sessionId) {
        return Mono
                // 包装同步移除逻辑
                .fromRunnable(() -> {
                    removeAndClose(sessionId);
                    log.info("手动移除会话 sessionId={}", sessionId);
                })
                // 转换为Mono<Void>，符合响应式返回规范
                .then();
    }

    // ==========================================
    // 2. 编排层（统一删除入口）
    // ==========================================

    /**
     * 统一删除会话 + 释放底层资源
     * 核心：先从Map移除，再关闭Sink，避免资源泄漏
     *
     * @param sessionId 会话唯一标识
     */
    private void removeAndClose(String sessionId) {
        SessionConfigVO vo = activeSessions.remove(sessionId);
        if (vo != null) {
            closeSession(vo);
        }
    }

    /**
     * 校验会话有效性
     *
     * @param vo 会话配置VO
     * @return true=有效（活跃且未过期），false=无效
     */
    private boolean isSessionValid(SessionConfigVO vo) {
        return vo.isActive() && !vo.isExpired(SESSION_TIMEOUT_MINUTES);
    }

    // ==========================================
    // 3. 资源层（底层操作）
    // ==========================================

    /**
     * 关闭会话并释放Sink资源
     *
     * @param vo 会话配置VO
     */
    private void closeSession(SessionConfigVO vo) {
        // 标记会话为非活跃：防止被再次使用
        vo.markInactive();

        // 尝试发送完成信号：终止SSE流
        Sinks.EmitResult result = vo
                .getSink()
                .tryEmitComplete();
        // 发送失败时记录警告：便于排查资源释放异常
        if (result.isFailure()) {
            log.warn("关闭Sink失败 sessionId={}, 结果={}", vo.getSessionId(), result);
        }
    }

    /**
     * 向Sink推送SSE事件（通用方法）
     *
     * @param sink  消息发送器
     * @param event 事件类型（如endpoint/message/error）
     * @param data  事件数据
     */
    private void emit(Sinks.Many<ServerSentEvent<String>> sink, String event, String data) {
        // 快速失败策略：推送失败时直接抛出异常，保证消息不丢失
        sink.emitNext(ServerSentEvent
                .<String>builder()
                .event(event) // 事件类型，前端可识别
                .data(data)   // 事件内容
                .build(), Sinks.EmitFailureHandler.FAIL_FAST);
    }

    // ==========================================
    // 4. 消息能力（为AI流式做准备）
    // ==========================================

    /**
     * 向指定会话推送业务消息
     *
     * @param sessionId 会话唯一标识
     * @param data      消息内容（AI流式返回数据）
     * @return Mono<Void> 空响应
     */
    public Mono<Void> sendMessage(String sessionId, String data) {
        return getSession(sessionId)
                // 会话不存在时抛出异常
                .switchIfEmpty(Mono.error(new RuntimeException("session不存在")))
                // 推送消息事件
                .doOnNext(vo -> emit(vo.getSink(), "message", data))
                .then();
    }

    /**
     * 向指定会话推送错误消息
     *
     * @param sessionId 会话唯一标识
     * @param error     错误信息
     * @return Mono<Void> 空响应
     */
    public Mono<Void> sendError(String sessionId, String error) {
        return getSession(sessionId)
                // 推送错误事件（会话不存在时返回空，不抛异常）
                .doOnNext(vo -> emit(vo.getSink(), "error", error))
                .then();
    }

    /**
     * 完成会话（主动关闭）
     *
     * @param sessionId 会话唯一标识
     * @return Mono<Void> 空响应
     */
    public Mono<Void> complete(String sessionId) {
        return Mono.fromRunnable(() -> removeAndClose(sessionId));
    }

    // ==========================================
    // 5. GC 清理（系统行为）
    // ==========================================

    /**
     * 清理过期会话（响应式）
     *
     * @return Mono<Void> 空响应
     */
    @Override
    public Mono<Void> cleanupExpiredSessions() {
        return Mono.fromRunnable(() -> {
                       // 清理前记录会话数，用于日志统计
                       int before = activeSessions.size();
                       // 收集所有过期会话ID，批量处理
                       List<String> expiredIds = new ArrayList<>();

                       // 遍历活跃会话池，筛选过期会话
                       for (Map.Entry<String, SessionConfigVO> entry : activeSessions.entrySet()) {
                           SessionConfigVO vo = entry.getValue();
                           if (!isSessionValid(vo)) {
                               expiredIds.add(entry.getKey());
                           }
                       }

                       // 批量移除并释放资源
                       expiredIds.forEach(this::removeAndClose);

                       // 计算清理数量，仅在有清理时打印日志
                       int cleaned = before - activeSessions.size();
                       if (cleaned > 0) {
                           log.info("GC清理完成 清理数量={}, 剩余会话数={}", cleaned, activeSessions.size());
                       }

                   })
                   // 指定线程池：避免阻塞响应式主线程
                   .subscribeOn(Schedulers.boundedElastic())
                   .then();
    }

    // ==========================================
    // 6. 生命周期
    // ==========================================

    /**
     * 服务初始化：启动会话过期清理定时任务
     */
    @Override
    public void afterPropertiesSet() {
        // 每5分钟执行一次过期清理
        Disposable task = Flux.interval(Duration.ofMinutes(5))
                              // 背压策略：丢弃溢出的定时任务，避免任务堆积
                              .onBackpressureDrop()
                              // 执行清理逻辑，异常时记录日志并继续
                              .flatMap(i -> cleanupExpiredSessions().onErrorResume(e -> {
                                  log.error("会话GC清理异常", e);
                                  return Mono.empty();
                              }))
                              .subscribe();

        // 将定时任务加入资源容器，便于销毁时统一释放
        disposables.add(task);

        log.info("Session管理服务启动完成");
    }

    /**
     * 服务关闭：清理所有会话（非响应式）
     */
    @Override
    public Mono<Void> shutdown() {
        return Mono
                .fromRunnable(() -> {
                    log.info("系统关闭，开始清理所有会话...");
                    // 批量清理所有活跃会话
                    activeSessions
                            .keySet()
                            .forEach(this::removeAndClose);
                })
                .then();
    }

    /**
     * 容器销毁：释放所有资源（适配Spring生命周期）
     */
    @Override
    public void destroy() {
        log.info("容器销毁，开始释放Session服务资源...");

        // 释放定时任务等响应式资源
        disposables.dispose();

        // 阻塞式执行会话清理（最多等待10秒），保证资源释放完成
        try {
            shutdown().block(Duration.ofSeconds(10));
        } catch (Exception e) {
            log.error("Session服务关闭异常", e);
        }

        log.info("Session服务资源释放完成");
    }
}