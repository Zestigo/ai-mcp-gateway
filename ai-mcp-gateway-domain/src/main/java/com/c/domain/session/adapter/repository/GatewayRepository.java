package com.c.domain.session.adapter.repository;

import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import com.c.domain.session.model.valobj.gateway.McpGatewayProtocolConfigVO;
import com.c.domain.session.model.valobj.gateway.McpGatewayToolConfigVO;

import java.util.List;

/**
 * 网关配置仓储接口
 * 定义网关配置、工具配置、协议配置的数据查询能力
 *
 * @author cyh
 * @date 2026/03/26
 */
public interface GatewayRepository {

    /**
     * 根据网关ID查询基础网关配置
     *
     * @param gatewayId 网关ID
     * @return 网关基础配置
     */
    McpGatewayConfigVO queryMcpGatewayConfigByGatewayId(String gatewayId);

    /**
     * 根据网关ID查询工具字段映射配置列表
     *
     * @param gatewayId 网关ID
     * @return 工具配置列表
     */
    List<McpGatewayToolConfigVO> queryMcpGatewayToolConfigListByGatewayId(String gatewayId);

    /**
     * 根据网关ID查询协议调用配置
     *
     * @param gatewayId 网关ID
     * @return 网关协议配置
     */
    McpGatewayProtocolConfigVO queryMcpGatewayProtocolConfig(String gatewayId);

}