package com.c.domain.session.model.valobj.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * MCP 网关工具字段映射配置值对象
 * 存储请求/响应的字段映射关系，用于动态构建 MCP Schema
 *
 * @author cyh
 * @date 2026/03/25
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class McpGatewayToolConfigVO {

    /** 网关唯一标识 */
    private String gatewayId;

    /** 关联工具ID */
    private Long toolId;

    /** 映射类型：request-请求参数 / response-响应数据 */
    private String mappingType;

    /** 父级路径，用于构建嵌套结构（根节点为null） */
    private String parentPath;

    /** 字段名称 */
    private String fieldName;

    /** MCP 完整路径（如：request.city、response.data.name） */
    private String mcpPath;

    /** MCP 数据类型：string/number/boolean/object/array */
    private String mcpType;

    /** MCP 字段描述 */
    private String mcpDescription;

    /** 是否必填：1-是，0-否 */
    private Integer isRequired;

    /** 同级字段排序号 */
    private Integer sortOrder;

}