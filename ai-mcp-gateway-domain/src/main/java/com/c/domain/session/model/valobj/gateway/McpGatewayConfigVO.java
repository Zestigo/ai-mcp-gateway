package com.c.domain.session.model.valobj.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * MCP网关基础配置值对象
 * 封装网关核心标识、名称、描述、状态等基础信息
 *
 * @author cyh
 * @date 2026/03/26
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class McpGatewayConfigVO {

    /** 网关唯一标识 */
    private String gatewayId;

    /** 网关名称 */
    private String gatewayName;

    /** 网关描述信息 */
    private String gatewayDescription;

    /** 网关版本号 */
    private String gatewayVersion;

    /** 网关启用状态：0-禁用，1-启用 */
    private Integer status;
}