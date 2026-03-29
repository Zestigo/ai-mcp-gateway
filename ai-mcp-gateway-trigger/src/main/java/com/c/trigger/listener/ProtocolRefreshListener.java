package com.c.trigger.listener;

import com.c.domain.protocol.adapter.repository.ProtocolCacheRetriever;
import com.c.domain.protocol.model.valobj.ProtocolRefreshMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 协议刷新监听器
 * 监听Redis广播消息，实现网关本地协议缓存的自动刷新
 *
 * @author cyh
 * @date 2026/03/29
 */
@Slf4j
@Component
public class ProtocolRefreshListener implements CommandLineRunner {

    /** Redisson客户端，用于订阅Redis主题 */
    @Resource
    private RedissonClient redissonClient;

    /** 协议缓存检索服务，执行缓存刷新操作 */
    @Resource
    private ProtocolCacheRetriever protocolCacheRetriever;

    /** 协议刷新广播通道名称 */
    private static final String PROTOCOL_REFRESH_CHANNEL = "mcp_protocol_refresh_channel";

    /**
     * 项目启动后自动注册Redis主题监听器
     *
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        // 获取Redis主题对象
        RTopic topic = redissonClient.getTopic(PROTOCOL_REFRESH_CHANNEL);
        // 注册消息监听器
        topic.addListener(ProtocolRefreshMessage.class, (channel, msg) -> {
            log.info("网关节点[Consumer]收到刷新指令，涉及协议数: {}", msg
                    .getProtocolIds()
                    .size());

            // 遍历协议ID集合，逐个刷新本地缓存
            for (Long protocolId : msg.getProtocolIds()) {
                try {
                    // 强制刷新单个协议的本地缓存
                    protocolCacheRetriever.forceRefresh(protocolId);
                } catch (Exception e) {
                    log.error("刷新本地缓存失败: protocolId={}", protocolId, e);
                }
            }
        });
    }
}