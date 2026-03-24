package com.c.infrastructure.adapter.repository;

import com.c.domain.session.adapter.repository.SessionRepository;
import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import com.c.infrastructure.dao.McpGatewayDao;
import com.c.infrastructure.dao.McpProtocolRegistryDao;
import com.c.infrastructure.dao.po.McpGatewayPO;
import com.c.infrastructure.dao.po.McpProtocolRegistryPO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * 会话仓储实现类：查询网关与工具的关联配置
 *
 * @author cyh
 * @date 2026/03/23
 */
@Slf4j
@Repository
public class SessionRepositoryImpl implements SessionRepository {

    /** 网关配置数据访问接口 */
    @Resource
    private McpGatewayDao mcpGatewayDao;

    /** MCP工具注册数据访问接口 */
    @Resource
    private McpProtocolRegistryDao mcpProtocolRegistryDao;

    /**
     * 根据网关ID查询网关协议配置
     *
     * @param gatewayId 网关唯一标识
     * @return 网关协议配置VO（网关/工具关联信息），无数据时返回null
     */
    @Override
    public McpGatewayConfigVO queryMcpGatewayConfigByGatewayId(String gatewayId) {
        // 查询网关基础配置
        McpGatewayPO mcpGatewayPO = mcpGatewayDao.queryMcpGatewayByGatewayId(gatewayId);
        if (null == mcpGatewayPO) return null;

        // 查询网关关联的工具注册配置（网关与工具为1:1关联）
        McpProtocolRegistryPO mcpProtocolRegistryPO =
                mcpProtocolRegistryDao.queryMcpProtocolRegistryByGatewayId(gatewayId);
        if (null == mcpProtocolRegistryPO) return null;

        // 组装网关配置VO返回
        return McpGatewayConfigVO
                .builder()
                .gatewayId(mcpGatewayPO.getGatewayId())
                .gatewayName(mcpGatewayPO.getGatewayName())
                .toolId(mcpProtocolRegistryPO.getToolId())
                .toolName(mcpProtocolRegistryPO.getToolName())
                .toolDesc(mcpProtocolRegistryPO.getToolDescription())
                .toolVersion(mcpProtocolRegistryPO.getToolVersion())
                .build();
    }

}