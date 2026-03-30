package com.c.domain.protocol.service;

import com.c.domain.protocol.model.entity.StorageCommandEntity;

import java.util.List;

/**
 * 协议存储领域服务接口
 * 职责：执行协议持久化，并管理分布式事务中的本地消息一致性
 *
 * @author cyh
 * @date 2026/03/29
 */
public interface ProtocolStorage {

    /**
     * 执行协议数据持久化操作
     *
     * @param commandEntity 协议存储命令实体，包含待持久化的 VO 集合
     * @return 成功持久化的协议 ID 集合
     */
    List<Long> doStorage(StorageCommandEntity commandEntity);

    /**
     * 执行本地消息表的补偿重试逻辑
     */
    void retrySendMessages();

}