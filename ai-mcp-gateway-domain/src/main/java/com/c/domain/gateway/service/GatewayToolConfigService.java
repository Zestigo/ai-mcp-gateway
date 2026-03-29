package com.c.domain.gateway.service;

import com.c.domain.gateway.model.entity.GatewayToolConfigCommandEntity;

/**
 * 网关工具配置服务接口
 * 定义网关工具配置的核心业务操作标准
 *
 * @author cyh
 * @date 2026/03/29
 */
public interface GatewayToolConfigService {

    /**
     * 保存网关工具配置
     *
     * @param commandEntity 网关工具配置命令实体
     */
    void saveGatewayToolConfig(GatewayToolConfigCommandEntity commandEntity);

    /**
     * 更新网关工具协议配置
     *
     * @param commandEntity 网关工具配置命令实体
     */
    void updateGatewayToolProtocol(GatewayToolConfigCommandEntity commandEntity);

}