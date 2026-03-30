package com.c.cases.admin;

import com.c.domain.protocol.model.entity.StorageCommandEntity;

/**
 * 协议配置管理服务接口
 * 定义网关协议配置操作
 *
 * @author cyh
 * @date 2026/03/29
 */
public interface AdminProtocolService {

    /**
     * 保存网关协议配置
     *
     * @param commandEntity 存储命令实体
     */
    void saveGatewayProtocol(StorageCommandEntity commandEntity);

}