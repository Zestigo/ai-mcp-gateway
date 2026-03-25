package com.c.domain.session.model.valobj.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * MCP 网关基础配置值对象
 * 用于封装网关标识、名称及关联工具的核心配置信息
 *
 * @author cyh
 * @date 2026/03/25
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

    /** 关联工具唯一标识 */
    private Long toolId;

    /** MCP 工具名称 */
    private String toolName;

    /** MCP 工具描述信息 */
    private String toolDescription;

    /** MCP 工具版本号 */
    private String toolVersion;
}