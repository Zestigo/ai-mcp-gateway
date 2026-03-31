package com.c.infrastructure.adapter.repository;

import com.c.domain.session.adapter.repository.SessionRepository;
import com.c.domain.session.model.entity.McpSession;
import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import com.c.domain.session.model.valobj.gateway.McpToolConfigVO;
import com.c.domain.session.model.valobj.gateway.McpToolProtocolConfigVO;
import com.c.infrastructure.dao.*;
import com.c.infrastructure.dao.po.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP会话仓储数据库实现
 * 负责会话数据、网关配置、工具配置、协议配置的数据库查询与持久化
 *
 * @author cyh
 * @date 2026/03/29
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SessionRepositoryImpl implements SessionRepository {

    /** 会话数据访问对象 */
    private final McpSessionDao mcpSessionDao;

    /** 网关配置数据访问对象 */
    private final McpGatewayDao mcpGatewayDao;

    /** 网关工具配置数据访问对象 */
    private final McpGatewayToolDao mcpGatewayToolDao;

    /** 协议映射配置数据访问对象 */
    private final McpProtocolMappingDao mcpProtocolMappingDao;

    /** HTTP协议配置数据访问对象 */
    private final McpProtocolHttpDao mcpProtocolHttpDao;

    /**
     * 根据网关ID查询网关基础配置
     *
     * @param gatewayId 网关唯一标识
     * @return 网关配置视图对象，不存在返回null
     */
    @Override
    public McpGatewayConfigVO queryMcpGatewayConfigByGatewayId(String gatewayId) {
        // 查询网关持久化对象
        McpGatewayPO gatewayPO = mcpGatewayDao.queryByGatewayId(gatewayId);
        if (gatewayPO == null) {
            log.warn("未找到有效的网关配置, gatewayId: {}", gatewayId);
            return null;
        }
        // 转换为视图对象返回
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
     * 根据网关ID查询工具配置列表，包含协议映射信息
     *
     * @param gatewayId 网关唯一标识
     * @return 工具配置视图对象列表
     */
    @Override
    public List<McpToolConfigVO> queryMcpGatewayToolConfigListByGatewayId(String gatewayId) {
        // 查询工具配置列表
        List<McpGatewayToolPO> toolPOList = mcpGatewayToolDao.getToolsByGatewayId(gatewayId);
        if (toolPOList == null || toolPOList.isEmpty()) return Collections.emptyList();

        // 提取协议ID集合，去重并过滤空值
        List<Long> protocolIds = toolPOList
                .stream()
                .map(McpGatewayToolPO::getProtocolId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 批量查询协议映射配置
        List<McpProtocolMappingPO> allMappings = mcpProtocolMappingDao.queryByProtocolIds(protocolIds);

        // 按协议ID分组映射配置，构建映射关系
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
                                .mcpDescription(po.getMcpDescription())
                                .isRequired(po.getIsRequired())
                                .sortOrder(po.getSortOrder())
                                .build(), Collectors.toList())));

        // 转换工具配置为视图对象，填充协议映射信息
        return toolPOList
                .stream()
                .map(tool -> McpToolConfigVO
                        .builder()
                        .gatewayId(tool.getGatewayId())
                        .toolId(tool.getToolId())
                        .toolName(tool.getToolName())
                        .toolDescription(tool.getToolDescription())
                        .toolVersion(tool.getToolVersion())
                        .toolStatus(tool.getToolStatus())
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
     * @return 工具协议配置视图对象，不存在返回null
     */
    @Override
    public McpToolProtocolConfigVO queryMcpGatewayProtocolConfig(String gatewayId, String toolName) {
        // 构建查询参数
        McpGatewayToolPO mcpGatewayToolPOReq = McpGatewayToolPO
                .builder()
                .gatewayId(gatewayId)
                .toolName(toolName)
                .build();

        // 查询协议ID
        Long protocolId = mcpGatewayToolDao.queryToolProtocolIdByToolName(mcpGatewayToolPOReq);
        if (protocolId == null) return null;

        // 查询HTTP协议配置
        McpProtocolHttpPO httpPO = mcpProtocolHttpDao.queryByProtocolId(protocolId);
        if (httpPO == null) return null;

        // 构建并返回协议配置视图对象
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

    /**
     * 保存会话信息
     *
     * @param session 会话实体对象
     */
    @Override
    public void save(McpSession session) {
        // 空值校验
        if (session == null) return;

        // 构建会话持久化对象，时间类型转换处理
        McpSessionPO po = McpSessionPO
                .builder()
                .sessionId(session.getSessionId())
                .gatewayId(session.getGatewayId())
                .hostIp(session.getHostIp())
                .active(session.getActiveState())
                .timeoutSeconds(session.getTimeoutSeconds())
                .lastAccessTime(Date.from(session.getLastAccessTime()))
                .createTime(session.getCreateTime() != null ? Date.from(session.getCreateTime()) : new Date())
                .updateTime(new Date())
                .build();

        // 执行插入
        mcpSessionDao.insert(po);
    }

    /**
     * 根据会话ID查询会话信息
     *
     * @param sessionId 会话唯一标识
     * @return 会话实体对象，不存在返回null
     */
    @Override
    public McpSession find(String sessionId) {
        // 查询会话持久化对象
        McpSessionPO po = mcpSessionDao.selectBySessionId(sessionId);
        if (po == null) return null;

        // 转换为领域实体，时间类型空值保护
        return McpSession
                .builder()
                .sessionId(po.getSessionId())
                .gatewayId(po.getGatewayId())
                .hostIp(po.getHostIp())
                .active(po.getActive() == 1)
                .timeoutSeconds(po.getTimeoutSeconds())
                .lastAccessTime(po.getLastAccessTime() != null ? po
                        .getLastAccessTime()
                        .toInstant() : Instant.now())
                .createTime(po.getCreateTime() != null ? po
                        .getCreateTime()
                        .toInstant() : null)
                .build();
    }

    /**
     * 更新会话信息
     *
     * @param session 会话实体对象
     */
    @Override
    public void update(McpSession session) {
        // 空值校验
        if (session == null) return;

        // 构建更新对象
        McpSessionPO po = McpSessionPO
                .builder()
                .sessionId(session.getSessionId())
                .hostIp(session.getHostIp())
                .active(session.getActiveState())
                .timeoutSeconds(session.getTimeoutSeconds())
                .lastAccessTime(Date.from(session.getLastAccessTime()))
                .updateTime(new Date())
                .build();

        // 执行更新
        mcpSessionDao.update(po);
    }

    /**
     * 根据会话ID查询会话信息（标准方法）
     *
     * @param sessionId 会话唯一标识
     * @return 会话实体对象，不存在返回null
     */
    @Override
    public McpSession findBySessionId(String sessionId) {
        return find(sessionId);
    }

    /**
     * 根据会话ID删除会话
     *
     * @param sessionId 会话唯一标识
     */
    @Override
    public void deleteById(String sessionId) {
        mcpSessionDao.deleteBySessionId(sessionId);
    }

    /**
     * 移除指定会话
     *
     * @param sessionId 会话唯一标识
     */
    @Override
    public void remove(String sessionId) {
        deleteById(sessionId);
    }

    /**
     * 统计当前活跃会话数量
     *
     * @return 活跃会话数
     */
    @Override
    public long countActiveSessions() {
        return mcpSessionDao.countActiveSessions();
    }

    /**
     * 删除数据库中已过期的会话
     *
     * @return 删除的会话数量
     */
    @Override
    public int deleteExpiredSessions() {
        return mcpSessionDao.deleteExpiredSessions();
    }

    /**
     * 根据网关ID查询关联会话ID集合
     *
     * @param gatewayId 网关唯一标识
     * @return 会话ID集合
     */
    @Override
    public Set<String> findByGateway(String gatewayId) {
        List<McpSessionPO> poList = mcpSessionDao.selectByGatewayId(gatewayId);
        if (poList == null) return Collections.emptySet();
        return poList
                .stream()
                .map(McpSessionPO::getSessionId)
                .collect(Collectors.toSet());
    }

    /**
     * 查询所有会话实体
     *
     * @return 会话实体集合
     */
    @Override
    public Collection<McpSession> findAll() {
        List<McpSessionPO> poList = mcpSessionDao.selectAll();
        if (poList == null) return Collections.emptyList();

        return poList
                .stream()
                .map(po -> McpSession
                        .builder()
                        .sessionId(po.getSessionId())
                        .gatewayId(po.getGatewayId())
                        .hostIp(po.getHostIp())
                        .active(po.getActive() == 1)
                        .timeoutSeconds(po.getTimeoutSeconds())
                        .lastAccessTime(po.getLastAccessTime() != null ? po
                                .getLastAccessTime()
                                .toInstant() : Instant.now())
                        .createTime(po.getCreateTime() != null ? po
                                .getCreateTime()
                                .toInstant() : null)
                        .build())
                .toList();
    }

    /**
     * 统计会话总数量
     *
     * @return 会话总数
     */
    @Override
    public long count() {
        return mcpSessionDao.countAll();
    }
}