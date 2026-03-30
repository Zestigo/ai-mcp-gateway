package com.c.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * MCP网关数据库持久化对象
 * 映射数据库网关配置表，存储网关标识、名称、版本、状态、鉴权开关等核心配置信息
 *
 * @author cyh
 * @date 2026/03/29
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

    /** 网关鉴权开关：0-关闭鉴权，1-开启鉴权 */
    private Integer auth;

    /** 记录创建时间 */
    private Date createTime;

    /** 记录更新时间 */
    private Date updateTime;

}