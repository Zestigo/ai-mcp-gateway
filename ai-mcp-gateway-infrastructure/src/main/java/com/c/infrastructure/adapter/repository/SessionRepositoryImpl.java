package com.c.infrastructure.adapter.repository;

import com.c.domain.session.adapter.repository.SessionRepository;
import com.c.domain.session.model.entity.McpSession;
import com.c.infrastructure.dao.McpSessionDao;
import com.c.infrastructure.dao.po.McpSessionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * MCP 会话仓储的数据库持久化实现
 * 负责完成会话实体与数据库记录之间的转换，以及会话数据的增删改查操作
 *
 * @author cyh
 * @date 2026/03/25
 */
@Repository
@RequiredArgsConstructor
public class SessionRepositoryImpl implements SessionRepository {

    /** 会话数据访问对象 */
    private final McpSessionDao mcpSessionDao;

    /**
     * 将会话实体保存到数据库
     *
     * @param session 待保存的会话实体
     */
    @Override
    public void save(McpSession session) {
        // 构建会话持久化对象，完成领域实体到PO的转换
        McpSessionPO po = McpSessionPO
                .builder()
                .sessionId(session.getSessionId())
                .gatewayId(session.getGatewayId())
                .active(session.isActive() ? 1 : 0)
                .timeoutSeconds(session.getTimeoutSeconds())
                .lastAccessTime(Date.from(session.getLastAccessTime()))
                .createTime(new Date())
                .updateTime(new Date())
                .build();
        // 调用DAO执行插入操作
        mcpSessionDao.insert(po);
    }

    /**
     * 根据会话ID查询会话信息
     *
     * @param sessionId 会话唯一标识
     * @return 会话实体，不存在则返回null
     */
    @Override
    public McpSession find(String sessionId) {
        // 根据会话ID查询数据库记录
        McpSessionPO po = mcpSessionDao.selectBySessionId(sessionId);
        if (po == null) return null;

        // 将持久化对象转换为领域实体
        McpSession session = new McpSession(po.getSessionId(), po.getGatewayId(), po.getTimeoutSeconds());
        session.setActive(po.getActive() == 1);
        session.setLastAccessTime(po
                .getLastAccessTime()
                .toInstant());
        return session;
    }

    /**
     * 根据会话ID精确查询会话信息
     *
     * @param sessionId 会话唯一标识
     * @return 会话实体，不存在则返回null
     */
    @Override
    public McpSession findBySessionId(String sessionId) {
        return find(sessionId);
    }

    /**
     * 更新数据库中的会话信息
     *
     * @param session 待更新的会话实体
     */
    @Override
    public void update(McpSession session) {
        // 构建更新用的持久化对象
        McpSessionPO po = McpSessionPO
                .builder()
                .sessionId(session.getSessionId())
                .active(session.isActive() ? 1 : 0)
                .timeoutSeconds(session.getTimeoutSeconds())
                .lastAccessTime(Date.from(session.getLastAccessTime()))
                .updateTime(new Date())
                .build();
        // 执行数据库更新
        mcpSessionDao.update(po);
    }

    /**
     * 根据会话ID从数据库删除会话记录
     *
     * @param sessionId 会话唯一标识
     */
    @Override
    public void deleteById(String sessionId) {
        mcpSessionDao.deleteBySessionId(sessionId);
    }

    /**
     * 移除指定会话（委托给deleteById实现）
     *
     * @param sessionId 会话唯一标识
     */
    @Override
    public void remove(String sessionId) {
        deleteById(sessionId);
    }

    /**
     * 统计当前系统中处于活跃状态的会话总数
     *
     * @return 活跃会话数量
     */
    @Override
    public long countActiveSessions() {
        return mcpSessionDao.countActiveSessions();
    }

    /**
     * 删除数据库中已过期或已失效的会话记录
     *
     * @return 被删除的会话数量
     */
    @Override
    public int deleteExpiredSessions() {
        return mcpSessionDao.deleteExpiredSessions();
    }

    /**
     * 根据网关ID查询关联的所有会话ID
     *
     * @param gatewayId 网关唯一标识
     * @return 会话ID集合
     */
    @Override
    public Set<String> findByGateway(String gatewayId) {
        // 查询网关下所有会话记录
        List<McpSessionPO> poList = mcpSessionDao.selectByGatewayId(gatewayId);
        Set<String> sessionIds = new HashSet<>();
        // 提取会话ID并封装为集合返回
        for (McpSessionPO po : poList) {
            sessionIds.add(po.getSessionId());
        }
        return sessionIds;
    }

    /**
     * 查询数据库中的所有会话记录
     *
     * @return 会话实体集合
     */
    @Override
    public Collection<McpSession> findAll() {
        // 查询所有会话记录
        List<McpSessionPO> poList = mcpSessionDao.selectAll();
        List<McpSession> sessions = new ArrayList<>();
        // 转换为领域实体列表
        for (McpSessionPO po : poList) {
            McpSession session = new McpSession(po.getSessionId(), po.getGatewayId(), po.getTimeoutSeconds());
            session.setActive(po.getActive() == 1);
            session.setLastAccessTime(po
                    .getLastAccessTime()
                    .toInstant());
            sessions.add(session);
        }
        return sessions;
    }

    /**
     * 统计会话总数量
     *
     * @return 会话总数
     */
    @Override
    public long count() {
        return mcpSessionDao.countActiveSessions();
    }
}