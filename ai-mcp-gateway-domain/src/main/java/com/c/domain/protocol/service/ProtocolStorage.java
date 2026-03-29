package com.c.domain.protocol.service;

import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;

import java.util.List;

/**
 * 协议存储服务接口
 * 定义协议解析结果的持久化契约
 *
 * @author cyh
 * @date 2026/03/28
 */
public interface ProtocolStorage {

    /**
     * 批量存储网关协议配置
     *
     * @param gatewayId   网关唯一标识
     * @param protocolVOs 协议配置对象集合
     */
    void store(String gatewayId, List<HTTPProtocolVO> protocolVOs);

    /**
     * 覆盖更新网关协议配置
     *
     * @param gatewayId   网关唯一标识
     * @param protocolVOs 最新协议配置对象集合
     */
    default void update(String gatewayId, List<HTTPProtocolVO> protocolVOs) {
        store(gatewayId, protocolVOs);
    }
}