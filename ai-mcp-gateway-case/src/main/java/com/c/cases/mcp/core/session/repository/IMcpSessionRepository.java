package com.c.cases.mcp.core.session.repository;

import java.time.Duration;
import java.util.Optional;

/**
 * MCP会话仓储接口：定义会话数据的CRUD及生命周期管控标准
 * 作用：隔离会话存储底层实现（如内存/Redis），上层业务无需关注存储介质
 *
 * @author cyh
 * @date 2026/03/19
 */
public interface IMcpSessionRepository {

    /**
     * 创建并存储会话
     *
     * @param sessionId 会话唯一标识（全局唯一，用于定位会话）
     * @param session   会话对象（存储会话的配置、状态、SSE发射器等核心数据）
     */
    void saveSession(String sessionId, Object session);

    /**
     * 获取会话（泛型适配不同会话对象类型）
     *
     * @param sessionId 会话唯一标识
     * @param <T>       会话对象目标类型
     * @return Optional包装的会话对象（避免空指针，无数据时返回Optional.empty()）
     */
    <T> Optional<T> getSession(String sessionId);

    /**
     * 删除会话（释放会话资源）
     *
     * @param sessionId 会话唯一标识
     *                  说明：删除后会话数据不可恢复，通常在客户端断连/会话超时后调用
     */
    void removeSession(String sessionId);

    /**
     * 更新会话数据（支持会话状态流转/属性修改）
     *
     * @param sessionId 会话唯一标识
     * @param session   最新的会话对象（覆盖原有数据）
     *                  场景：如会话心跳续期、状态从"活跃"改为"闲置"等
     */
    void updateSession(String sessionId, Object session);

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话唯一标识
     * @return true-会话存在，false-会话不存在
     * 场景：执行会话操作前的前置校验，避免无效操作
     */
    boolean exists(String sessionId);

    /**
     * 设置会话过期时间（实现自动清理）
     *
     * @param sessionId 会话唯一标识
     * @param ttl       过期时长（Time To Live，如30分钟）
     *                  说明：过期后仓储层自动删除会话，无需业务层手动清理，防止内存泄漏
     */
    void expire(String sessionId, Duration ttl);
}