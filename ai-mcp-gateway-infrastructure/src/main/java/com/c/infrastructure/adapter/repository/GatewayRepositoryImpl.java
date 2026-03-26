package com.c.infrastructure.adapter.repository;

import com.c.domain.session.adapter.repository.GatewayRepository;
import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import com.c.domain.session.model.valobj.gateway.McpGatewayProtocolConfigVO;
import com.c.domain.session.model.valobj.gateway.McpGatewayToolConfigVO;
import com.c.infrastructure.dao.*;
import com.c.infrastructure.dao.po.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 网关配置仓储实现类
 * 聚合多表数据，为领域层提供统一的网关配置查询能力
 *
 * @author cyh
 * @date 2026/03/26
 */
@Repository
@RequiredArgsConstructor
public class GatewayRepositoryImpl implements GatewayRepository {

    /** 网关基础配置DAO */
    private final McpGatewayDao mcpGatewayDao;

    /** 协议注册配置DAO */
    private final McpProtocolRegistryDao mcpProtocolRegistryDao;

    /** 协议映射配置DAO */
    private final McpProtocolMappingDao mcpProtocolMappingDao;

    /** 网关授权配置DAO */
    private final McpGatewayAuthDao mcpGatewayAuthDao;

    /**
     * 根据网关ID查询基础网关配置
     *
     * @param gatewayId 网关ID
     * @return 网关基础配置值对象
     */
    @Override
    public McpGatewayConfigVO queryMcpGatewayConfigByGatewayId(String gatewayId) {
        McpGatewayPO gatewayPO = mcpGatewayDao.queryMcpGatewayByGatewayId(gatewayId);
        McpProtocolRegistryPO registryPO = mcpProtocolRegistryDao.queryMcpProtocolRegistryByGatewayId(gatewayId);

        if (registryPO == null || gatewayPO == null) return null;

        return McpGatewayConfigVO
                .builder()
                .gatewayId(gatewayId)
                .gatewayName(gatewayPO.getGatewayName())
                .toolId(registryPO.getToolId())
                .toolName(registryPO.getToolName())
                .toolDescription(registryPO.getToolDescription())
                .toolVersion(registryPO.getToolVersion())
                .build();
    }

    /**
     * 根据网关ID查询工具配置列表
     *
     * @param gatewayId 网关ID
     * @return 工具配置值对象列表
     */
    @Override
    public List<McpGatewayToolConfigVO> queryMcpGatewayToolConfigListByGatewayId(String gatewayId) {
        McpProtocolMappingPO reqPO = new McpProtocolMappingPO();
        reqPO.setGatewayId(gatewayId);

        List<McpProtocolMappingPO> poList = mcpProtocolMappingDao.queryMcpGatewayToolConfigList(reqPO);

        return poList
                .stream()
                .map(po -> McpGatewayToolConfigVO
                        .builder()
                        .gatewayId(po.getGatewayId())
                        .toolId(po.getToolId())
                        .mappingType(po.getMappingType())
                        .parentPath(po.getParentPath())
                        .fieldName(po.getFieldName())
                        .mcpPath(po.getMcpPath())
                        .mcpType(po.getMcpType())
                        .mcpDescription(po.getMcpDescription())
                        .isRequired(po.getIsRequired())
                        .sortOrder(po.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 查询指定网关的协议配置信息
     * 聚合授权信息与协议注册信息，构建HTTP调用配置
     *
     * @param gatewayId 网关ID
     * @return 网关协议配置值对象
     */
    @Override
    public McpGatewayProtocolConfigVO queryMcpGatewayProtocolConfig(String gatewayId) {
        // 查询网关授权配置
        McpGatewayAuthPO authPO = mcpGatewayAuthDao.queryByGatewayId(gatewayId);
        if (authPO == null) return null;

        // 查询协议注册配置（包含HTTP请求核心参数）
        McpProtocolRegistryPO registryPO = mcpProtocolRegistryDao.queryMcpProtocolRegistryByGatewayId(gatewayId);
        if (registryPO == null) return null;

        // 构建HTTP请求配置
        McpGatewayProtocolConfigVO.HTTPConfig httpConfig = McpGatewayProtocolConfigVO.HTTPConfig
                .builder()
                .httpUrl(registryPO.getHttpUrl())
                .httpMethod(registryPO.getHttpMethod())
                .httpHeaders(registryPO.getHttpHeaders())
                .timeout(registryPO.getTimeout() != null ? registryPO.getTimeout() : 3000)
                .build();

        return McpGatewayProtocolConfigVO
                .builder()
                .gatewayId(authPO.getGatewayId())
                .httpConfig(httpConfig)
                .build();
    }
}