package com.c.domain.gateway.adapter.repository;

import com.c.domain.gateway.model.entity.GatewayConfigCommandEntity;
import com.c.domain.gateway.model.entity.GatewayToolConfigCommandEntity;

/**
 * 网关仓储服务接口
 * 定义网关配置、网关工具配置的数据持久化操作标准
 *
 * @author cyh
 * @date 2026/03/29
 */
public interface GatewayRepository {

    /**
     * 保存网关基础配置信息
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

    /**
     * 保存网关工具配置信息
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