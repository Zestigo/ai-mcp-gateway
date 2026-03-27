package com.c.infrastructure.adapter.repository;

import com.c.domain.session.adapter.repository.SessionRepository;
import com.c.domain.session.model.entity.McpSession;
import com.c.infrastructure.dao.McpSessionDao;
import com.c.infrastructure.dao.po.McpSessionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * MCP会话仓储数据库实现类
 *
 * @author cyh
 * @date 2026/03/27
 */
@Repository
@RequiredArgsConstructor
public class SessionRepositoryImpl implements SessionRepository {

    /** MCP会话数据库操作接口 */
    private final McpSessionDao mcpSessionDao;

    /**
     * 将会话领域实体转换为PO并持久化到数据库
     *
     * @param session 会话领域实体
     */
    @Override
    public void save(McpSession session) {
        if (session == null) return;

        McpSessionPO po = McpSessionPO
                .builder()
                .sessionId(session.getSessionId())
                .gatewayId(session.getGatewayId())
                .hostIp(session.getHostIp())
                .active(session.getActiveState())
                .timeoutSeconds(session.getTimeoutSeconds())
                .lastAccessTime(Date.from(session.getLastAccessTime()))
                .createTime(session.getCreateTime() != null ? Date.from(session.getCreateTime()) : new Date())
                .updateTime(new Date())
                .build();

        mcpSessionDao.insert(po);
    }

    /**
     * 根据会话ID查询PO并转换为领域实体
     *
     * @param sessionId 会话唯一标识
     * @return 会话领域实体，不存在返回null
     */
    @Override
    public McpSession find(String sessionId) {
        McpSessionPO po = mcpSessionDao.selectBySessionId(sessionId);
        if (po == null) return null;

        return McpSession
                .builder()
                .sessionId(po.getSessionId())
                .gatewayId(po.getGatewayId())
                .hostIp(po.getHostIp())
                .active(po.getActive() == 1)
                .timeoutSeconds(po.getTimeoutSeconds())
                .lastAccessTime(po
                        .getLastAccessTime()
                        .toInstant())
                .createTime(po.getCreateTime() != null ? po
                        .getCreateTime()
                        .toInstant() : null)
                .build();
    }

    /**
     * 更新会话信息，同步更新数据库记录
     *
     * @param session 会话领域实体
     */
    @Override
    public void update(McpSession session) {
        if (session == null) return;

        McpSessionPO po = McpSessionPO
                .builder()
                .sessionId(session.getSessionId())
                .hostIp(session.getHostIp())
                .active(session.getActiveState())
                .timeoutSeconds(session.getTimeoutSeconds())
                .lastAccessTime(Date.from(session.getLastAccessTime()))
                .updateTime(new Date())
                .build();

        mcpSessionDao.update(po);
    }

    /**
     * 根据会话ID查询会话（代理find方法）
     *
     * @param sessionId 会话唯一标识
     * @return 会话领域实体
     */
    @Override
    public McpSession findBySessionId(String sessionId) {
        return find(sessionId);
    }

    /**
     * 根据会话ID删除数据库会话记录
     *
     * @param sessionId 会话唯一标识
     */
    @Override
    public void deleteById(String sessionId) {
        mcpSessionDao.deleteBySessionId(sessionId);
    }

    /**
     * 移除会话（代理deleteById方法）
     *
     * @param sessionId 会话唯一标识
     */
    @Override
    public void remove(String sessionId) {
        deleteById(sessionId);
    }

    /**
     * 统计活跃会话数量
     *
     * @return 活跃会话数
     */
    @Override
    public long countActiveSessions() {
        return mcpSessionDao.countActiveSessions();
    }

    /**
     * 删除数据库中过期会话记录
     *
     * @return 删除记录数
     */
    @Override
    public int deleteExpiredSessions() {
        return mcpSessionDao.deleteExpiredSessions();
    }

    /**
     * 根据网关ID查询关联会话ID集合
     *
     * @param gatewayId 网关唯一标识
     * @return 会话ID集合
     */
    @Override
    public Set<String> findByGateway(String gatewayId) {
        List<McpSessionPO> poList = mcpSessionDao.selectByGatewayId(gatewayId);
        Set<String> sessionIds = new HashSet<>();
        if (poList != null) {
            for (McpSessionPO po : poList) {
                sessionIds.add(po.getSessionId());
            }
        }
        return sessionIds;
    }

    /**
     * 查询所有会话并转换为领域实体集合
     *
     * @return 会话实体集合
     */
    @Override
    public Collection<McpSession> findAll() {
        List<McpSessionPO> poList = mcpSessionDao.selectAll();
        List<McpSession> sessions = new ArrayList<>();
        if (poList != null) {
            for (McpSessionPO po : poList) {
                McpSession session = McpSession
                        .builder()
                        .sessionId(po.getSessionId())
                        .gatewayId(po.getGatewayId())
                        .hostIp(po.getHostIp())
                        .active(po.getActive() == 1)
                        .timeoutSeconds(po.getTimeoutSeconds())
                        .lastAccessTime(po
                                .getLastAccessTime()
                                .toInstant())
                        .createTime(po.getCreateTime() != null ? po
                                .getCreateTime()
                                .toInstant() : null)
                        .build();
                sessions.add(session);
            }
        }
        return sessions;
    }

    /**
     * 统计会话总数量（复用活跃会话统计）
     *
     * @return 会话总数
     */
    @Override
    public long count() {
        return mcpSessionDao.countActiveSessions();
    }
}