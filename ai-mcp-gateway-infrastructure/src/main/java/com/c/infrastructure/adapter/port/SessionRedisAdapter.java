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
 * 会话Redis存储适配器
 * 实现领域层定义的Redis端口，负责会话关系维护及跨节点消息广播
 *
 * @author cyh
 * @date 2026/03/29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionRedisAdapter implements SessionRedisPort {

    /** Redis订阅主题前缀：用于跨机器节点消息推送 */
    private static final String MCP_TOPIC_PREFIX = "mcp_node_";

    /** Redis业务键前缀：标识网关与会话映射关系 */
    private static final String SESSION_KEY_PREFIX = "mcp:gateway:sessions:";

    /** Redis模板对象 */
    private final StringRedisTemplate redisTemplate;

    /**
     * 绑定网关与会话关联关系
     *
     * @param gatewayId 网关唯一标识
     * @param sessionId 会话唯一标识
     */
    @Override
    public void bindSession(String gatewayId, String sessionId) {
        String key = SESSION_KEY_PREFIX + gatewayId;
        // 将会话ID加入集合
        redisTemplate
                .opsForSet()
                .add(key, sessionId);
        // 设置过期时间，防止内存泄漏
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
    }

    /**
     * 获取网关下所有会话ID
     *
     * @param gatewayId 网关唯一标识
     * @return 会话ID集合
     */
    @Override
    public Set<String> getSessions(String gatewayId) {
        Set<String> sessions = redisTemplate
                .opsForSet()
                .members(SESSION_KEY_PREFIX + gatewayId);
        return CollectionUtils.isEmpty(sessions) ? Collections.emptySet() : sessions;
    }

    /**
     * 跨节点消息发布
     *
     * @param hostIp    目标机器IP
     * @param sessionId 目标会话ID
     * @param message   协议消息体
     */
    @Override
    public void publish(String hostIp, String sessionId, McpSchemaVO.JSONRPCMessage message) {
        // 构建目标主题
        String targetTopic = MCP_TOPIC_PREFIX + hostIp;
        // 封装消息对象
        RemotePushMessage remoteMsg = new RemotePushMessage(sessionId, message);

        // 序列化并发布消息
        try {
            String jsonPayload = JSON.toJSONString(remoteMsg);
            redisTemplate.convertAndSend(targetTopic, jsonPayload);
        } catch (Exception e) {
            log.error("跨节点消息发布失败 targetTopic: {} sessionId: {}", targetTopic, sessionId, e);
        }
    }

    /**
     * 移除网关与会话关联关系
     *
     * @param gatewayId 网关唯一标识
     * @param sessionId 会话唯一标识
     */
    @Override
    public void removeSession(String gatewayId, String sessionId) {
        redisTemplate
                .opsForSet()
                .remove(SESSION_KEY_PREFIX + gatewayId, sessionId);
    }

    /**
     * 内部跨节点通讯对象
     * 用于Redis Pub/Sub传输，承载会话ID和协议消息
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RemotePushMessage {
        /** 目标会话唯一标识 */
        private String sessionId;
        /** MCP协议消息内容 */
        private McpSchemaVO.JSONRPCMessage message;
    }
}