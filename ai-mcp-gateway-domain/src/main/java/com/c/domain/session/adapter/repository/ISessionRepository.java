package com.c.domain.session.adapter.repository;

import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;

/**
 * 会话仓储接口：提供网关配置数据查询能力
 *
 * @author cyh
 * @date 2026/03/23
 */
public interface ISessionRepository {

    /**
     * 根据网关ID查询网关配置
     *
     * @param gatewayId 网关唯一标识
     * @return 网关协议配置值对象
     */
    McpGatewayConfigVO queryMcpGatewayConfigByGatewayId(String gatewayId);

}