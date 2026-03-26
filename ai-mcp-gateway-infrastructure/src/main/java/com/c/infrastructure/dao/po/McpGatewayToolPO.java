package com.c.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * MCP网关工具持久化对象
 * 存储网关与工具的关联配置信息
 *
 * @author cyh
 * @date 2026/03/26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpGatewayToolPO {

    /** 数据库自增主键 */
    private Long id;

    /** 所属网关唯一标识 */
    private String gatewayId;

    /** 工具唯一标识 */
    private Long toolId;

    /** MCP工具名称 */
    private String toolName;

    /** 工具类型 */
    private String toolType;

    /** 工具描述信息 */
    private String toolDescription;

    /** 工具版本号 */
    private String toolVersion;

    /** 关联协议ID */
    private Long protocolId;

    /** 协议类型 */
    private String protocolType;

    /** 记录创建时间 */
    private Date createTime;

    /** 记录更新时间 */
    private Date updateTime;

}