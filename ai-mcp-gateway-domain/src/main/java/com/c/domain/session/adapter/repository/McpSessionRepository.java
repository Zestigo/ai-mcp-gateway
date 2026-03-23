package com.c.domain.session.adapter.repository;

import com.c.domain.session.model.entity.McpSession;

import java.util.Collection;
import java.util.Set;

/**
 * 会话仓储接口
 *
 * @author cyh
 * @date 2026/03/24
 */
public interface McpSessionRepository {

    /**
     * 保存会话
     * @param session 会话实体
     */
    void save(McpSession session);

    /**
     * 根据会话ID查询会话
     * @param sessionId 会话唯一标识
     * @return 会话实体
     */
    McpSession find(String sessionId);

    /**
     * 移除会话
     * @param sessionId 会话唯一标识
     */
    void remove(String sessionId);

    /**
     * 根据网关ID查询关联的会话ID集合
     * @param gatewayId 网关唯一标识
     * @return 会话ID集合
     */
    Set<String> findByGateway(String gatewayId);

    /**
     * 查询所有会话
     * @return 会话实体集合
     */
    Collection<McpSession> findAll();

    /**
     * 统计会话总数
     * @return 会话数量
     */
    long count();
}