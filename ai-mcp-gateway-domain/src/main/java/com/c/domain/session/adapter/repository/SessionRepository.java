package com.c.domain.session.adapter.repository;

import com.c.domain.session.model.entity.McpSession;

import java.util.Collection;
import java.util.Set;

/**
 * MCP会话仓储接口
 * 定义会话数据持久化标准操作，屏蔽底层存储实现
 * 负责会话的增删改查、活跃统计、过期清理
 *
 * @author cyh
 * @date 2026/03/27
 */
public interface SessionRepository {

    /**
     * 保存会话实体
     *
     * @param session 会话实体
     */
    void save(McpSession session);

    /**
     * 根据会话ID查询会话
     *
     * @param sessionId 会话唯一标识
     * @return 会话实体，不存在返回null
     */
    McpSession find(String sessionId);

    /**
     * 根据会话ID查询会话（标准查询方法）
     *
     * @param sessionId 会话唯一标识
     * @return 会话实体，不存在返回null
     */
    McpSession findBySessionId(String sessionId);

    /**
     * 移除指定会话
     *
     * @param sessionId 会话唯一标识
     */
    void remove(String sessionId);

    /**
     * 根据网关ID查询关联会话ID集合
     *
     * @param gatewayId 网关唯一标识
     * @return 会话ID集合
     */
    Set<String> findByGateway(String gatewayId);

    /**
     * 查询所有会话实体
     *
     * @return 会话实体集合
     */
    Collection<McpSession> findAll();

    /**
     * 统计会话总数量
     *
     * @return 会话总数
     */
    long count();

    /**
     * 更新会话信息
     *
     * @param session 会话实体
     */
    void update(McpSession session);

    /**
     * 根据会话ID删除会话
     *
     * @param sessionId 会话唯一标识
     */
    void deleteById(String sessionId);

    /**
     * 统计当前活跃会话数量
     *
     * @return 活跃会话数
     */
    long countActiveSessions();

    /**
     * 删除数据库中已过期的会话
     *
     * @return 删除的会话数量
     */
    int deleteExpiredSessions();

}