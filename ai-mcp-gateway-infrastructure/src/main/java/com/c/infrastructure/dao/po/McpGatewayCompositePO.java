package com.c.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 网关聚合查询持久化对象
 * 承载mcp_gateway与mcp_gateway_auth联表查询结果，用于认证授权聚合数据映射
 *
 * @author cyh
 * @date 2026/03/27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpGatewayCompositePO {

    /** 网关唯一标识，查询条件字段 */
    private String gatewayId;

    /** 接口认证密钥，查询条件字段 */
    private String apiKey;

    /** 网关鉴权开关，映射联表查询g_auth字段 */
    private Integer auth;

    /** 网关运行状态，映射联表查询g_status字段 */
    private Integer gatewayStatus;

    /** 密钥授权状态，映射联表查询ga_status字段 */
    private Integer authStatus;

    /** 请求限流阈值，映射联表查询ga_rate_limit字段 */
    private Long rateLimit;

    /** 认证过期时间，映射联表查询ga_expire_time字段 */
    private Date expireTime;

    /**
     * 聚合查询专用构造函数
     *
     * @param gatewayId 网关唯一标识
     * @param apiKey    接口认证密钥
     */
    public McpGatewayCompositePO(String gatewayId, String apiKey) {
        this.gatewayId = gatewayId;
        this.apiKey = apiKey;
    }
}