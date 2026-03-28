package com.c.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis 发布订阅消息发布器
 * 提供基于 Redis Pub/Sub 模式的消息推送能力，用于 MCP 会话通道消息广播
 *
 * @author cyh
 * @date 2026/03/27
 */
@Service
@RequiredArgsConstructor
public class RedisPubSubPublisher {

    /** Redis 字符串模板，用于执行消息发布操作 */
    private final StringRedisTemplate redisTemplate;

    /**
     * 向指定 MCP 会话通道发布消息
     *
     * @param sessionId 会话唯一标识，用于构建 Redis 通道名称
     * @param msg       待发布的消息内容
     */
    public void publish(String sessionId, String msg) {
        // 构建会话专属通道并发布消息，通道格式：mcp:session:{sessionId}
        redisTemplate.convertAndSend("mcp:session:" + sessionId, msg);
    }
}