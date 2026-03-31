package com.c.infrastructure.adapter.event;

import com.c.domain.protocol.adapter.repository.EventPublisher;
import com.c.domain.protocol.model.valobj.ProtocolRefreshMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/**
 * 事件发布实现类
 * 基于 Redis 的事件发布实现，负责广播协议刷新事件到集群
 * 
 * @author cyh
 * @date 2026/03/31
 */
@Slf4j
@Component
public class EventPublisherImpl implements EventPublisher {

    /** Redisson 客户端，用于发布事件到 Redis 主题 */
    @Resource
    private RedissonClient redissonClient;

    /** 协议刷新事件通道名称 */
    private static final String PROTOCOL_REFRESH_CHANNEL = "mcp_protocol_refresh_channel";

    /**
     * 发布协议刷新事件
     * 
     * @param message 协议刷新消息
     */
    @Override
    public void publishProtocolRefresh(ProtocolRefreshMessage message) {
        try {
            RTopic topic = redissonClient.getTopic(PROTOCOL_REFRESH_CHANNEL);
            topic.publish(message);
            log.info("广播协议刷新事件成功: {}", message.getProtocolIds());
        } catch (Exception e) {
            log.error("广播协议刷新事件异常", e);
        }
    }
}