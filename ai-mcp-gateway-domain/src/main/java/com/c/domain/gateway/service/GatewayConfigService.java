package com.c.domain.gateway.service;

import com.c.domain.gateway.model.entity.GatewayConfigCommandEntity;

/**
 * 网关配置服务接口
 * 定义网关配置的核心业务操作标准
 *
 * @author cyh
 * @date 2026/03/29
 */
public interface GatewayConfigService {

    /**
     * 保存网关配置
     *
     * @param commandEntity 网关配置命令实体
     */
    void saveGatewayConfig(GatewayConfigCommandEntity commandEntity);

    /**
     * 更新网关认证状态
     *
     * @param commandEntity 网关配置命令实体
     */
    void updateGatewayAuthStatus(GatewayConfigCommandEntity commandEntity);

}