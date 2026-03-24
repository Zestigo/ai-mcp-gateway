package com.c.infrastructure.adapter.repository;

import com.c.domain.session.adapter.repository.McpSessionRepository;
import com.c.domain.session.model.entity.McpSession;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话仓储实现（内存）
 *
 * @author cyh
 * @date 2026/03/24
 */
@Repository
public class McpSessionRepositoryImpl implements McpSessionRepository {

    /** 会话存储 */
    private final Map<String, McpSession> sessions = new ConcurrentHashMap<>();
    /** 网关-会话索引 */
    private final Map<String, Set<String>> gatewayIndex = new ConcurrentHashMap<>();

    /**
     * 保存会话
     *
     * @param session 会话实体
     */
    @Override
    public void save(McpSession session) {
        // 保存会话
        sessions.put(session.getSessionId(), session);

        // 维护网关与会话的关联索引
        gatewayIndex
                .computeIfAbsent(session.getGatewayId(), k -> ConcurrentHashMap.newKeySet())
                .add(session.getSessionId());
    }

    /**
     * 根据ID查询会话
     *
     * @param sessionId 会话标识
     * @return 会话实体
     */
    @Override
    public McpSession find(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 移除会话
     *
     * @param sessionId 会话标识
     */
    @Override
    public void remove(String sessionId) {
        McpSession session = sessions.remove(sessionId);
        if (session == null) return;

        // 清理网关索引
        Set<String> set = gatewayIndex.get(session.getGatewayId());
        if (set != null) {
            set.remove(sessionId);
            // 空集合清理索引
            if (set.isEmpty()) {
                gatewayIndex.remove(session.getGatewayId());
            }
        }
    }

    /**
     * 根据网关ID查询会话ID集合
     *
     * @param gatewayId 网关标识
     * @return 会话ID集合
     */
    @Override
    public Set<String> findByGateway(String gatewayId) {
        Set<String> set = gatewayIndex.get(gatewayId);
        return set == null ? Collections.emptySet() : Set.copyOf(set);
    }

    /**
     * 查询所有会话
     *
     * @return 会话集合
     */
    @Override
    public Collection<McpSession> findAll() {
        return new ArrayList<>(sessions.values());
    }

    /**
     * 统计会话总数
     *
     * @return 会话数量
     */
    @Override
    public long count() {
        return sessions.size();
    }
}