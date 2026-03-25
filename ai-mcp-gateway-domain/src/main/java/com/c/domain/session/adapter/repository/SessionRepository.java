package com.c.domain.session.adapter.repository;

import com.c.domain.session.model.entity.McpSession;

import java.util.Collection;
import java.util.Set;

/**
 * MCP 会话仓储接口
 * 负责会话的持久化、查询、更新、过期清理等生命周期管理
 *
 * @author cyh
 * @date 2026/03/25
 */
public interface SessionRepository {

    /**
     * 保存会话
     */
    void save(McpSession session);

    /**
     * 根据会话ID查询会话
     */
    McpSession find(String sessionId);

    /**
     * 根据会话ID查询会话（标准方法）
     */
    McpSession findBySessionId(String sessionId);

    /**
     * 移除会话
     */
    void remove(String sessionId);

    /**
     * 根据网关ID查询会话ID集合
     */
    Set<String> findByGateway(String gatewayId);

    /**
     * 查询所有会话
     */
    Collection<McpSession> findAll();

    /**
     * 统计会话总数
     */
    long count();

    /**
     * 更新会话信息
     */
    void update(McpSession session);

    /**
     * 根据ID删除会话
     */
    void deleteById(String sessionId);

    /**
     * 统计活跃会话数量
     */
    long countActiveSessions();

    /**
     * 删除已过期会话
     */
    int deleteExpiredSessions();

}