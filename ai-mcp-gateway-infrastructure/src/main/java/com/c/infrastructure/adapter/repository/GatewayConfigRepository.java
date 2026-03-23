package com.c.infrastructure.adapter.repository;

import com.c.domain.session.adapter.repository.McpGatewayConfigRepository;
import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网关配置仓储（内存实现）
 *
 * @author cyh
 * @date 2026/03/24
 */
@Repository
public class GatewayConfigRepository implements McpGatewayConfigRepository {

    /** 配置存储 */
    private final Map<String, McpGatewayConfigVO> store = new ConcurrentHashMap<>();

    /**
     * 根据网关ID查询配置
     *
     * @param gatewayId 网关标识
     * @return 网关配置
     */
    @Override
    public McpGatewayConfigVO queryMcpGatewayConfigByGatewayId(String gatewayId) {
        // 不存在则构建默认配置
        return store.computeIfAbsent(gatewayId, this::buildDefaultConfig);
    }

    /**
     * 构建默认网关配置
     *
     * @param gatewayId 网关标识
     * @return 网关配置
     */
    private McpGatewayConfigVO buildDefaultConfig(String gatewayId) {
        return McpGatewayConfigVO
                .builder()
                .gatewayId(gatewayId)
                .gatewayName("Gateway-" + gatewayId)
                .toolId(Math.abs((long) gatewayId.hashCode()))
                .toolName("mcp-default-tool")
                .toolDesc("Default tool binding for gateway " + gatewayId)
                .toolVersion("1.0.0")
                .build();
    }
}