package com.c.domain.protocol.adapter.repository;

import com.c.domain.protocol.model.valobj.ProtocolRefreshMessage;

/**
 * 领域事件发布接口
 * 定义领域事件发布行为，实现依赖倒置
 *
 * @author cyh
 * @date 2026/03/29
 */
public interface EventPublisher {

    /**
     * 发布协议刷新事件
     *
     * @param message 协议刷新消息对象
     */
    void publishProtocolRefresh(ProtocolRefreshMessage message);

}