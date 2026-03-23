package com.c.domain.session.adapter.repository;

import java.util.Set;

/**
 * 会话 Redis 存储接口
 *
 * @author cyh
 * @date 2026/03/24
 */
public interface SessionRedisPort {

    /**
     * 绑定网关与会话的关联关系
     * @param gatewayId 网关唯一标识
     * @param sessionId 会话唯一标识
     */
    void bindSession(String gatewayId, String sessionId);

    /**
     * 获取网关下所有会话ID
     * @param gatewayId 网关唯一标识
     * @return 会话ID集合
     */
    Set<String> getSessions(String gatewayId);

    /**
     * 移除网关与会话的关联关系
     * @param gatewayId 网关唯一标识
     * @param sessionId 会话唯一标识
     */
    void removeSession(String gatewayId, String sessionId);
}