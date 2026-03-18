package com.c.domain.session.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 会话配置领域值对象
 * 封装 SSE (Server-Sent Events) 响应流及其生命周期元数据
 *
 * @author cyh
 * @date 2026/03/18
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionConfigVO {

    /** 会话唯一标识符 */
    private String sessionId;

    /** 响应式热流汇聚点，用于多播推送 SSE 消息 */
    private Sinks.Many<ServerSentEvent<String>> sink;

    /** 会话创建时间 */
    private Instant createTime;

    /** 最后一次访问时间，volatile 确保在不同调度线程间的内存可见性 */
    private volatile Instant lastAccessedTime;

    /** 会话活跃状态位，由清理任务或断开信号驱动 */
    private volatile boolean active;

    public SessionConfigVO(String sessionId, Sinks.Many<ServerSentEvent<String>> sink) {
        this.sessionId = sessionId;
        this.sink = sink;
        this.createTime = Instant.now();
        this.lastAccessedTime = Instant.now();
        this.active = true;
    }

    /** 标记会话失效，拦截后续流推送 */
    public void markInactive() {
        this.active = false;
    }

    /** 刷新最后访问时间，用于滑动窗口过期校验 */
    public void updateLastAccessed() {
        this.lastAccessedTime = Instant.now();
    }

    /**
     * 执行 TTL 过期判定
     *
     * @param timeoutMinutes 超时阈值（分钟）
     * @return 是否超过指定时长未活跃
     */
    public boolean isExpired(long timeoutMinutes) {
        return lastAccessedTime.isBefore(Instant
                .now()
                .minus(timeoutMinutes, ChronoUnit.MINUTES));
    }

}