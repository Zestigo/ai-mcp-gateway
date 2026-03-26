package com.c.infrastructure.adapter.repository;

import com.c.domain.session.adapter.repository.GatewayRepository;
import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import com.c.domain.session.model.valobj.gateway.McpToolConfigVO;
import com.c.domain.session.model.valobj.gateway.McpToolProtocolConfigVO;
import com.c.infrastructure.dao.*;
import com.c.infrastructure.dao.po.McpGatewayPO;
import com.c.infrastructure.dao.po.McpGatewayToolPO;
import com.c.infrastructure.dao.po.McpProtocolHttpPO;
import com.c.infrastructure.dao.po.McpProtocolMappingPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网关配置仓储实现类
 * 聚合多表数据，提供网关、工具、协议、HTTP配置的统一查询能力
 * 实现数据库PO到领域VO的转换，屏蔽数据层细节
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class GatewayRepositoryImpl implements GatewayRepository {

    /** 网关基础配置DAO */
    private final McpGatewayDao mcpGatewayDao;

    /** 网关工具配置DAO */
    private final McpGatewayToolDao mcpGatewayToolDao;

    /** 协议字段映射DAO */
    private final McpProtocolMappingDao mcpProtocolMappingDao;

    /** 网关授权配置DAO */
    private final McpGatewayAuthDao mcpGatewayAuthDao;

    /** HTTP协议配置DAO */
    private final McpProtocolHttpDao mcpProtocolHttpDao;

    /**
     * 根据网关ID查询网关基础配置
     *
     * @param gatewayId 网关唯一标识
     * @return 网关基础配置VO，无数据返回null
     */
    @Override
    public McpGatewayConfigVO queryMcpGatewayConfigByGatewayId(String gatewayId) {
        // 根据网关ID查询数据库配置
        McpGatewayPO gatewayPO = mcpGatewayDao.queryMcpGatewayByGatewayId(gatewayId);
        // 配置为空时打印警告并返回
        if (gatewayPO == null) {
            log.warn("未找到有效的网关配置, gatewayId: {}", gatewayId);
            return null;
        }
        // 转换为领域层VO对象返回
        return McpGatewayConfigVO
                .builder()
                .gatewayId(gatewayPO.getGatewayId())
                .gatewayName(gatewayPO.getGatewayName())
                .gatewayDescription(gatewayPO.getGatewayDescription())
                .gatewayVersion(gatewayPO.getGatewayVersion())
                .status(gatewayPO.getStatus())
                .build();
    }

    /**
     * 根据网关ID查询工具配置列表
     *
     * @param gatewayId 网关唯一标识
     * @return 工具配置列表，无数据返回空集合
     */
    @Override
    public List<McpToolConfigVO> queryMcpGatewayToolConfigListByGatewayId(String gatewayId) {
        // 获取网关下所有有效工具配置
        List<McpGatewayToolPO> toolPOList = mcpGatewayToolDao.queryEffectiveTools(gatewayId);
        // 无工具数据直接返回空集合
        if (toolPOList.isEmpty()) return new ArrayList<>();

        // 提取协议ID集合，用于批量查询映射配置（优化N+1查询）
        List<Long> protocolIds = toolPOList
                .stream()
                .map(McpGatewayToolPO::getProtocolId)
                .distinct()
                .toList();

        // 批量查询所有协议对应的字段映射配置
        List<McpProtocolMappingPO> allMappings = mcpProtocolMappingDao.queryByProtocolIds(protocolIds);

        // 将映射配置按协议ID分组，方便后续工具匹配
        Map<Long, List<McpToolProtocolConfigVO.ProtocolMapping>> mappingGroup = allMappings
                .stream()
                .collect(Collectors.groupingBy(McpProtocolMappingPO::getProtocolId,
                        Collectors.mapping(po -> McpToolProtocolConfigVO.ProtocolMapping
                                .builder()
                                .mappingType(po.getMappingType())
                                .parentPath(po.getParentPath())
                                .fieldName(po.getFieldName())
                                .mcpPath(po.getMcpPath())
                                .mcpType(po.getMcpType())
                                .McpDescription(po.getMcpDescription())
                                .isRequired(po.getIsRequired())
                                .sortOrder(po.getSortOrder())
                                .build(), Collectors.toList())));

        // 组装工具配置与协议映射，返回最终结果
        return toolPOList
                .stream()
                .map(tool -> McpToolConfigVO
                        .builder()
                        .gatewayId(tool.getGatewayId())
                        .toolId(tool.getToolId())
                        .toolName(tool.getToolName())
                        .toolDescription(tool.getToolDescription())
                        .toolVersion(tool.getToolVersion())
                        .mcpToolProtocolConfigVO(McpToolProtocolConfigVO
                                .builder()
                                .requestProtocolMappings(mappingGroup.getOrDefault(tool.getProtocolId(),
                                        new ArrayList<>()))
                                .build())
                        .build())
                .toList();
    }

    /**
     * 根据网关ID和工具名称查询协议调用配置
     *
     * @param gatewayId 网关唯一标识
     * @param toolName  工具名称
     * @return 工具协议配置VO，无数据返回null
     */
    @Override
    public McpToolProtocolConfigVO queryMcpGatewayProtocolConfig(String gatewayId, String toolName) {
        // 构建查询参数
        McpGatewayToolPO mcpGatewayToolPOReq = McpGatewayToolPO
                .builder()
                .gatewayId(gatewayId)
                .toolName(toolName)
                .build();

        // 查询对应的协议ID
        Long protocolId = mcpGatewayToolDao.queryToolProtocolIdByToolName(mcpGatewayToolPOReq);
        if (protocolId == null) return null;

        // 根据协议ID查询HTTP配置信息
        McpProtocolHttpPO httpPO = mcpProtocolHttpDao.queryMcpProtocolHttpByProtocolId(protocolId);
        if (httpPO == null) return null;

        // 构建并返回协议配置对象
        return McpToolProtocolConfigVO
                .builder()
                .httpConfig(McpToolProtocolConfigVO.HTTPConfig
                        .builder()
                        .httpUrl(httpPO.getHttpUrl())
                        .httpHeaders(httpPO.getHttpHeaders())
                        .httpMethod(httpPO.getHttpMethod())
                        .timeout(httpPO.getTimeout())
                        .build())
                .build();
    }

}