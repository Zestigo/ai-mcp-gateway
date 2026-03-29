package com.c.domain.session.adapter.repository;

import com.c.domain.session.model.valobj.McpSchemaVO;

import java.util.Set;

/**
 * 会话Redis存储与通讯接口
 * 负责：会话路由绑定、跨节点消息分发
 *
 * @author cyh
 * @date 2026/03/29
 */
public interface SessionRedisPort {

    /**
     * 绑定网关与会话的关联关系
     *
     * @param gatewayId 网关唯一标识
     * @param sessionId 会话唯一标识
     */
    void bindSession(String gatewayId, String sessionId);

    /**
     * 获取网关下所有会话ID
     *
     * @param gatewayId 网关唯一标识
     * @return 会话ID集合
     */
    Set<String> getSessions(String gatewayId);

    /**
     * 移除网关与会话的关联关系
     *
     * @param gatewayId 网关唯一标识
     * @param sessionId 会话唯一标识
     */
    void removeSession(String gatewayId, String sessionId);

    /**
     * 跨节点消息分发
     *
     * @param hostIp    目标机器IP
     * @param sessionId 目标会话ID
     * @param message   协议消息体
     */
    void publish(String hostIp, String sessionId, McpSchemaVO.JSONRPCMessage message);
}