package com.c.infrastructure.adapter.repository;

import com.c.domain.session.adapter.repository.GatewayRepository;
import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import com.c.domain.session.model.valobj.gateway.McpGatewayToolConfigVO;
import com.c.infrastructure.dao.McpGatewayDao;
import com.c.infrastructure.dao.McpProtocolMappingDao;
import com.c.infrastructure.dao.McpProtocolRegistryDao;
import com.c.infrastructure.dao.po.McpGatewayPO;
import com.c.infrastructure.dao.po.McpProtocolMappingPO;
import com.c.infrastructure.dao.po.McpProtocolRegistryPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关配置仓储的数据库实现
 * 负责从数据库加载网关基础信息、工具注册信息、协议映射信息，并转换为领域值对象
 *
 * @author cyh
 * @date 2026/03/25
 */
@Repository
@RequiredArgsConstructor
public class GatewayRepositoryImpl implements GatewayRepository {

    /** 网关基础配置数据访问对象 */
    private final McpGatewayDao mcpGatewayDao;

    /** 协议注册信息数据访问对象 */
    private final McpProtocolRegistryDao mcpProtocolRegistryDao;

    /** 协议字段映射数据访问对象 */
    private final McpProtocolMappingDao mcpProtocolMappingDao;

    /**
     * 根据网关ID查询网关完整配置信息
     *
     * @param gatewayId 网关唯一标识
     * @return 网关配置值对象，不存在则返回null
     */
    @Override
    public McpGatewayConfigVO queryMcpGatewayConfigByGatewayId(String gatewayId) {
        // 查询网关基础信息
        McpGatewayPO gatewayPO = mcpGatewayDao.queryMcpGatewayByGatewayId(gatewayId);
        // 查询网关对应的工具注册信息
        McpProtocolRegistryPO registryPO = mcpProtocolRegistryDao.queryMcpProtocolRegistryByGatewayId(gatewayId);

        // 关键数据不存在则直接返回空
        if (registryPO == null || gatewayPO == null) return null;

        // 构建并返回网关配置值对象
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
     * 根据网关ID查询工具协议字段映射配置列表
     *
     * @param gatewayId 网关唯一标识
     * @return 工具字段映射配置列表
     */
    @Override
    public List<McpGatewayToolConfigVO> queryMcpGatewayToolConfigListByGatewayId(String gatewayId) {
        // 构建查询参数
        McpProtocolMappingPO reqPO = new McpProtocolMappingPO();
        reqPO.setGatewayId(gatewayId);

        // 查询协议映射配置列表
        List<McpProtocolMappingPO> poList = mcpProtocolMappingDao.queryMcpGatewayToolConfigList(reqPO);
        List<McpGatewayToolConfigVO> voList = new ArrayList<>();

        // 将持久化对象转换为领域值对象
        for (McpProtocolMappingPO po : poList) {
            voList.add(McpGatewayToolConfigVO
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
                    .build());
        }
        return voList;
    }
}