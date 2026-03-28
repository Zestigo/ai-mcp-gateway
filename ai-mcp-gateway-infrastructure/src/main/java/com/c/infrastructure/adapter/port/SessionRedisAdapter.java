package com.c.infrastructure.adapter.port;

import com.alibaba.fastjson.JSON;
import com.c.domain.session.adapter.repository.SessionRedisPort;
import com.c.domain.session.model.valobj.McpSchemaVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

/**
 * 会话Redis存储适配器
 *
 * @author cyh
 * @date 2026/03/27
 */
@Service
@RequiredArgsConstructor
public class SessionRedisAdapter implements SessionRedisPort {

    /** Redis消息主题前缀 */
    private static final String MCP_TOPIC_PREFIX = "mcp_node_";
    /** Redis操作模板 */
    private final StringRedisTemplate redisTemplate;
    /** Redis键前缀 */
    private static final String PREFIX = "mcp:gateway:";

    /**
     * 绑定网关与会话
     *
     * @param gatewayId 网关标识
     * @param sessionId 会话标识
     */
    @Override
    public void bindSession(String gatewayId, String sessionId) {
        // 使用Set存储网关下的会话
        redisTemplate
                .opsForSet()
                .add(PREFIX + gatewayId, sessionId);
    }

    /**
     * 获取网关下所有会话
     *
     * @param gatewayId 网关标识
     * @return 会话ID集合
     */
    @Override
    public Set<String> getSessions(String gatewayId) {
        Set<String> set = redisTemplate
                .opsForSet()
                .members(PREFIX + gatewayId);
        return set == null ? Collections.emptySet() : set;
    }

    /**
     * 跨节点消息分发
     *
     * @param hostIp    目标机器IP
     * @param sessionId 目标会话ID
     * @param message   协议消息体
     */
    @Override
    public void publish(String hostIp, String sessionId, McpSchemaVO.JSONRPCMessage message) {
        String targetTopic = MCP_TOPIC_PREFIX + hostIp;

        // 封装内部传输对象
        RemotePushMessage remoteMsg = new RemotePushMessage(sessionId, message);

        // 发送Redis广播
        redisTemplate.convertAndSend(targetTopic, JSON.toJSONString(remoteMsg));
    }

    /**
     * 移除网关与会话关联
     *
     * @param gatewayId 网关标识
     * @param sessionId 会话标识
     */
    @Override
    public void removeSession(String gatewayId, String sessionId) {
        redisTemplate
                .opsForSet()
                .remove(PREFIX + gatewayId, sessionId);
    }

    /**
     * 内部跨节点通讯对象
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RemotePushMessage {
        /** 会话唯一标识 */
        private String sessionId;
        /** MCP协议消息体 */
        private McpSchemaVO.JSONRPCMessage message;
    }
}