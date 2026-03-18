package com.c.domain.session.service;

import com.c.domain.session.model.valobj.SessionConfigVO;
import reactor.core.publisher.Mono;

/**
 * 会话管理领域服务接口
 * 负责网关会话（Gateway Session）的全生命周期管控：
 * 1. 动态构建：基于网关元数据初始化响应式管道（Sinks）。
 * 2. 状态检索：维护活跃会话的内存映射与心跳感知。
 * 3. 资源回收：执行过期会话清理及系统级优雅停机。
 */
public interface ISessionManagementService {

    /**
     * 创建响应式会话
     * 初始化包含消息发射器（Sink）与配置元数据的会话对象，并建立内存索引。
     *
     * @param gatewayId 网关唯一标识
     * @return 封装会话元数据的 Mono 序列
     */
    Mono<SessionConfigVO> createSession(String gatewayId);

    /**
     * 获取活跃会话配置
     * 检索指定标识的会话。若会话已标记失效、过期或不存在，则返回 Empty Mono。
     *
     * @param sessionId 会话唯一标识
     * @return 匹配的会话对象（Mono 包装）
     */
    Mono<SessionConfigVO> getSession(String sessionId);

    /**
     * 显式注销会话
     * 执行逻辑移除并强制切断底层网络流信号（Complete），防止 Socket 句柄泄露。
     *
     * @param sessionId 会话唯一标识
     * @return 异步完成信号
     */
    Mono<Void> removeSession(String sessionId);

    /**
     * 自动清理过期会话
     * 扫描内存容器，强制回收由于客户端异常断连（半开连接）导致的不活跃冗余资源。
     *
     * @return 清理任务执行完毕的完成信号
     */
    Mono<Void> cleanupExpiredSessions();

    /**
     * 系统级优雅下线
     * 强制终结所有存活会话的信号流并清空容器，确保 JVM 安全退出。
     *
     * @return 资源全量回收的完成信号
     */
    Mono<Void> shutdown();

}