package com.c.domain.auth.adapter.repository;

import com.c.domain.auth.model.entity.LicenseCommandEntity;
import com.c.domain.auth.model.valobj.McpGatewayAuthVO;
import com.c.domain.auth.model.valobj.McpGatewayCompositeVO;
import com.c.domain.auth.model.valobj.enums.AuthStatusEnum;
import reactor.core.publisher.Mono;

/**
 * 认证授权仓储层接口
 * 遵循DDD架构规范，定义网关认证数据访问标准接口
 * 包含授权查询、注册、限流、聚合鉴权等数据操作能力
 *
 * @author cyh
 * @date 2026/03/27
 */
public interface AuthRepository {

    /**
     * 查询网关有效授权配置数量
     *
     * @param gatewayId 网关唯一标识
     * @return 有效授权记录条数
     */
    int queryEffectiveGatewayAuthCount(String gatewayId);

    /**
     * 查询网关有效授权配置信息
     *
     * @param commandEntity 证书校验命令实体
     * @return 网关授权值对象，无数据返回null
     */
    McpGatewayAuthVO queryEffectiveGatewayAuthInfo(LicenseCommandEntity commandEntity);

    /**
     * 插入网关授权配置信息
     *
     * @param mcpGatewayAuthVO 网关授权值对象
     */
    int insert(McpGatewayAuthVO mcpGatewayAuthVO);

    /**
     * 查询网关授权校验开关状态
     *
     * @param gatewayId 网关唯一标识
     * @return 网关校验配置枚举
     */
    AuthStatusEnum.GatewayConfig queryGatewayAuthStatus(String gatewayId);

    /**
     * 聚合查询网关全量认证信息
     * 采用双层缓存架构，响应式返回聚合认证数据
     *
     * @param gatewayId 网关唯一标识
     * @param apiKey    API访问密钥
     * @return 响应式聚合认证值对象
     */
    Mono<McpGatewayCompositeVO> queryCompositeAuth(String gatewayId, String apiKey);

    /**
     * 执行分布式限流计数判定
     *
     * @param gatewayId     网关唯一标识
     * @param apiKey        API访问密钥
     * @param limit         单位时间最大允许访问次数
     * @param windowSeconds 限流时间窗口（秒）
     * @return true=触发限流，false=允许访问
     */
    boolean isRateLimited(String gatewayId, String apiKey, int limit, int windowSeconds);
}