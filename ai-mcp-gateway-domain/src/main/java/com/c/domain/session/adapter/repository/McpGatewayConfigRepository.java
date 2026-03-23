package com.c.domain.session.adapter.repository;

import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;

/**
 * 网关配置查询仓储接口
 *
 * @author cyh
 * @date 2026/03/24
 */
public interface McpGatewayConfigRepository {

    /**
     * 根据网关ID查询网关配置
     * @param gatewayId 网关唯一标识
     * @return 网关配置值对象
     */
    McpGatewayConfigVO queryMcpGatewayConfigByGatewayId(String gatewayId);
}