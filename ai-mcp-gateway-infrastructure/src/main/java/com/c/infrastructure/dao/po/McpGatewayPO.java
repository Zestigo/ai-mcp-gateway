package com.c.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * MCP网关基础信息持久化对象
 * 对应数据库MCP网关配置表，用于存储网关的基础标识、名称、描述等核心基础信息
 *
 * @author cyh
 * @date 2026/03/25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpGatewayPO {

    /** 数据库自增主键ID */
    private Long id;

    /** 网关唯一标识，作为网关的业务主键 */
    private String gatewayId;

    /** 网关名称，用于标识和展示网关 */
    private String gatewayName;

    /** 网关描述，说明网关的用途和功能范围 */
    private String gatewayDescription;

    /** 网关状态，0-禁用，1-启用，控制网关是否可用 */
    private Integer status;

    /** 记录创建时间 */
    private Date createTime;

    /** 记录更新时间 */
    private Date updateTime;

}