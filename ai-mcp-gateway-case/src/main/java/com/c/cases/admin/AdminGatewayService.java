package com.c.cases.admin;

import com.c.domain.gateway.model.entity.GatewayConfigCommandEntity;
import com.c.domain.gateway.model.entity.GatewayToolConfigCommandEntity;

/**
 * 网关配置管理服务接口
 * 定义网关基础配置与工具配置操作
 *
 * @author cyh
 * @date 2026/03/29
 */
public interface AdminGatewayService {

    /**
     * 保存网关基础配置
     *
     * @param commandEntity 网关配置命令实体
     */
    void saveGatewayConfig(GatewayConfigCommandEntity commandEntity);

    /**
     * 保存网关工具配置
     *
     * @param commandEntity 网关工具配置命令实体
     */
    void saveGatewayToolConfig(GatewayToolConfigCommandEntity commandEntity);

}