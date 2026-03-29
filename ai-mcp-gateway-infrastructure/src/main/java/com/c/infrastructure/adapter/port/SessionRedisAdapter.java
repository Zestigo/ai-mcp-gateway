package com.c.infrastructure.adapter.port;

import com.alibaba.fastjson2.JSON;
import com.c.domain.session.adapter.repository.SessionRedisPort;
import com.c.domain.session.model.valobj.McpSchemaVO;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 会话 Redis 存储适配器
 * 职责：实现领域层定义的 Redis 端口，负责会话关系维护及跨节点消息广播。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionRedisAdapter implements SessionRedisPort {

    /** Redis 订阅主题前缀：用于跨机器节点消息推送 */
    private static final String MCP_TOPIC_PREFIX = "mcp_node_";

    /** Redis 业务键前缀：标识网关与会话映射关系 */
    private static final String SESSION_KEY_PREFIX = "mcp:gateway:sessions:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void bindSession(String gatewayId, String sessionId) {
        String key = SESSION_KEY_PREFIX + gatewayId;
        // 1. 绑定会话到 Set
        redisTemplate
                .opsForSet()
                .add(key, sessionId);

        // 2. 建议设置过期时间（例如 24 小时），防止极端情况下连接断开未触发 remove 导致的内存泄漏
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
    }

    @Override
    public Set<String> getSessions(String gatewayId) {
        Set<String> sessions = redisTemplate
                .opsForSet()
                .members(SESSION_KEY_PREFIX + gatewayId);
        return CollectionUtils.isEmpty(sessions) ? Collections.emptySet() : sessions;
    }

    @Override
    public void publish(String hostIp, String sessionId, McpSchemaVO.JSONRPCMessage message) {
        // 1. 确定目标机器节点的主题
        String targetTopic = MCP_TOPIC_PREFIX + hostIp;

        // 2. 封装内部传输对象
        RemotePushMessage remoteMsg = new RemotePushMessage(sessionId, message);

        // 3. 使用 Fastjson2 序列化并广播
        try {
            String jsonPayload = JSON.toJSONString(remoteMsg);
            redisTemplate.convertAndSend(targetTopic, jsonPayload);
        } catch (Exception e) {
            log.error("跨节点消息发布失败 targetTopic: {} sessionId: {}", targetTopic, sessionId, e);
        }
    }

    @Override
    public void removeSession(String gatewayId, String sessionId) {
        redisTemplate
                .opsForSet()
                .remove(SESSION_KEY_PREFIX + gatewayId, sessionId);
    }

    /**
     * 内部跨节点通讯对象
     * 用于在 Redis Pub/Sub 中传输，承载目标会话 ID 和 MCP 协议原文
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RemotePushMessage {
        /** 目标会话唯一标识 */
        private String sessionId;
        /** MCP 协议消息内容 */
        private McpSchemaVO.JSONRPCMessage message;
    }
}