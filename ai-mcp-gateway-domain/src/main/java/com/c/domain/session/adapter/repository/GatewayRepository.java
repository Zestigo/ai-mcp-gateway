package com.c.domain.session.adapter.repository;

import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import com.c.domain.session.model.valobj.gateway.McpGatewayToolConfigVO;

import java.util.List;

/**
 * 网关配置仓储接口
 * 负责网关基础信息、工具配置、协议映射配置的查询
 *
 * @author cyh
 * @date 2026/03/25
 */
public interface GatewayRepository {

    /**
     * 根据网关ID查询网关基础配置
     */
    McpGatewayConfigVO queryMcpGatewayConfigByGatewayId(String gatewayId);

    /**
     * 根据网关ID查询工具字段映射配置
     */
    List<McpGatewayToolConfigVO> queryMcpGatewayToolConfigListByGatewayId(String gatewayId);

}