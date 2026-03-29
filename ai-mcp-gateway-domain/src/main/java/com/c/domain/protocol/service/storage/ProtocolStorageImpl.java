package com.c.domain.protocol.service.storage;

import com.c.domain.protocol.adapter.repository.EventPublisher;
import com.c.domain.protocol.adapter.repository.ProtocolRepository;
import com.c.domain.protocol.model.entity.StorageCommandEntity;
import com.c.domain.protocol.model.valobj.ProtocolRefreshMessage;
import com.c.domain.protocol.service.ProtocolStorage;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 协议存储服务实现类
 * 负责协议配置数据持久化，并发布协议刷新领域事件
 *
 * @author cyh
 * @date 2026/03/29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProtocolStorageImpl implements ProtocolStorage {

    /** 协议仓储服务，执行数据持久化操作 */
    @Resource
    private ProtocolRepository protocolRepository;

    /** 事件发布器，发布协议刷新事件 */
    @Resource
    private EventPublisher eventPublisher;

    /**
     * 执行协议数据存储
     * 持久化后发布刷新事件，通知网关节点更新缓存
     *
     * @param commandEntity 协议存储命令实体
     * @return 存储后的协议ID集合
     */
    @Override
    public List<Long> doStorage(StorageCommandEntity commandEntity) {
        // 批量持久化协议数据到数据库
        List<Long> protocolIds = protocolRepository.batchSaveProtocols(commandEntity.getHttpProtocolVOS());

        // 协议存储成功，发布刷新事件通知网关节点
        if (!protocolIds.isEmpty()) {
            ProtocolRefreshMessage message = ProtocolRefreshMessage
                    .builder()
                    .eventType("REFRESH")
                    .protocolIds(protocolIds)
                    .timestamp(System.currentTimeMillis())
                    .build();

            // 发布协议刷新事件
            eventPublisher.publishProtocolRefresh(message);
        }

        return protocolIds;
    }
}