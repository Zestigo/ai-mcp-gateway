package com.c.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * MCP协议字段映射持久化对象
 * 对应数据库MCP映射配置表，用于存储MCP协议与HTTP接口之间的请求/响应字段映射规则
 * 支持嵌套结构构建、参数位置指定、数据类型定义等核心映射配置
 *
 * @author cyh
 * @date 2026/03/25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpProtocolMappingPO {

    /** 数据库自增主键ID */
    private Long id;

    /** 所属网关唯一标识，关联对应的网关配置 */
    private String gatewayId;

    /** 所属工具唯一标识，关联对应的MCP工具配置 */
    private Long toolId;

    /** 映射类型，request-请求参数映射，response-响应数据映射 */
    private String mappingType;

    /** 父级路径，用于构建嵌套数据结构，根节点字段该值为NULL */
    private String parentPath;

    /** 字段名称，对应业务字段标识，如city、company、name */
    private String fieldName;

    /** MCP协议完整路径，格式如xxxRequest01.city、xxxRequest01.company.name */
    private String mcpPath;

    /** MCP协议数据类型，支持string/number/boolean/object/array五种类型 */
    private String mcpType;

    /** MCP协议字段描述，说明字段的含义和用途 */
    private String mcpDescription;

    /** 是否必填字段，0-非必填，1-必填，用于生成JSON Schema的required数组 */
    private Integer isRequired;

    /** HTTP接口数据路径，用于解析/赋值接口数据，如company.name 或 data.result */
    private String httpPath;

    /** HTTP参数位置，仅对请求映射有效，支持body/query/path/header */
    private String httpLocation;

    /** 排序顺序，用于控制同级字段的展示和生成顺序 */
    private Integer sortOrder;

    /** 记录创建时间 */
    private Date createTime;

    /** 记录更新时间 */
    private Date updateTime;

}