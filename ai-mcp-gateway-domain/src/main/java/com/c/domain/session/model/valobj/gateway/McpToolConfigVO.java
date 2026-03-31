package com.c.domain.session.model.valobj.gateway;

import lombok.*;

/**
 * MCP工具配置值对象
 * 封装工具基础信息与关联的协议配置信息
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class McpToolConfigVO {

    /** 所属网关ID */
    private String gatewayId;

    /** 所属工具ID */
    private Long toolId;

    /** MCP工具名称 */
    private String toolName;

    /** 工具描述信息 */
    private String toolDescription;

    /** 工具版本号 */
    private String toolVersion;

    private Integer toolStatus;

    /** 工具关联协议配置 */
    private McpToolProtocolConfigVO mcpToolProtocolConfigVO;

}