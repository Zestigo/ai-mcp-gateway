package com.c.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * MCP网关持久化对象
 * 对应数据库网关配置表，存储网关基础信息与状态
 *
 * @author cyh
 * @date 2026/03/26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpGatewayPO {

    /** 数据库自增主键 */
    private Long id;

    /** 网关唯一业务标识 */
    private String gatewayId;

    /** 网关名称 */
    private String gatewayName;

    /** 网关描述信息 */
    private String gatewayDescription;

    /** 网关版本号 */
    private String gatewayVersion;

    /** 网关状态：0-禁用，1-启用 */
    private Integer status;

    /** 记录创建时间 */
    private Date createTime;

    /** 记录更新时间 */
    private Date updateTime;

}