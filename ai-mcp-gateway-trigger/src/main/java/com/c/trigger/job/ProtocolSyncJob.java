package com.c.trigger.job;

import com.c.domain.protocol.adapter.repository.ProtocolCacheRetriever;
import com.c.domain.protocol.adapter.repository.ProtocolRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 协议缓存同步定时任务
 * 分布式系统中用于补偿Redis广播消息丢失，保证本地缓存与数据库最终一致性
 *
 * @author cyh
 * @date 2026/03/29
 */
@Slf4j
@Component
public class ProtocolSyncJob {

    /** 协议仓储层，用于查询数据库中有效的协议ID */
    @Resource
    private ProtocolRepository protocolRepository;

    /** 协议缓存接口，用于执行本地缓存强制刷新 */
    @Resource
    private ProtocolCacheRetriever protocolCacheRetriever;

    /**
     * 定时执行协议缓存同步任务
     * 每10分钟执行一次，补偿Redis广播可能丢失的刷新消息
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void exec() {
        log.info("开始执行协议同步定时任务(一致性补偿)...");
        try {
            // 从数据库查询所有启用状态的协议ID列表，仅查询ID节约网络与内存开销
            List<Long> activeProtocolIds = protocolRepository.queryAllActiveProtocolIds();

            // 无有效协议，直接终止任务
            if (activeProtocolIds == null || activeProtocolIds.isEmpty()) {
                return;
            }

            // 遍历所有有效协议，逐个强制刷新本地缓存，保证缓存数据最新
            for (Long protocolId : activeProtocolIds) {
                protocolCacheRetriever.forceRefresh(protocolId);
            }

            log.info("协议同步定时任务执行完毕，共检查协议数: {}", activeProtocolIds.size());
        } catch (Exception e) {
            log.error("协议同步定时任务发生异常", e);
        }
    }
}