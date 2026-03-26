package com.c.domain.session.adapter.repository;

import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import com.c.domain.session.model.valobj.gateway.McpToolConfigVO;
import com.c.domain.session.model.valobj.gateway.McpToolProtocolConfigVO;

import java.util.List;

/**
 * 网关配置仓储接口
 * 定义网关基础配置、工具配置、协议配置的统一数据查询能力
 * 为领域层提供标准化的配置数据访问入口
 *
 * @author cyh
 * @date 2026/03/26
 */
public interface GatewayRepository {

    /**
     * 根据网关ID查询网关基础配置信息
     *
     * @param gatewayId 网关唯一标识
     * @return 网关基础配置值对象
     */
    McpGatewayConfigVO queryMcpGatewayConfigByGatewayId(String gatewayId);

    /**
     * 根据网关ID查询工具字段映射配置列表
     *
     * @param gatewayId 网关唯一标识
     * @return 工具配置值对象集合
     */
    List<McpToolConfigVO> queryMcpGatewayToolConfigListByGatewayId(String gatewayId);

    /**
     * 根据网关ID与工具名称查询协议调用配置
     *
     * @param gatewayId 网关唯一标识
     * @param toolName  工具名称
     * @return 工具协议配置值对象
     */
    McpToolProtocolConfigVO queryMcpGatewayProtocolConfig(String gatewayId, String toolName);

}