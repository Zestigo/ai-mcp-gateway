package com.c.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * MCP协议字段映射持久化对象
 * 存储MCP协议与接口之间的请求/响应字段映射规则，支持嵌套结构、参数位置与类型定义
 *
 * @author cyh
 * @date 2026/03/26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpProtocolMappingPO {

    /** 主键ID */
    private Long id;

    /** 协议ID */
    private Long protocolId;

    /** 映射类型：request-请求参数映射，response-响应数据映射 */
    private String mappingType;

    /** 父级路径，用于构建嵌套结构 */
    private String parentPath;

    /** 字段名称 */
    private String fieldName;

    /** MCP完整路径 */
    private String mcpPath;

    /** MCP数据类型：string/number/boolean/object/array */
    private String mcpType;

    /** MCP字段描述 */
    private String mcpDescription;

    /** 是否必填：0-否，1-是 */
    private Integer isRequired;

    /** 排序顺序 */
    private Integer sortOrder;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

}