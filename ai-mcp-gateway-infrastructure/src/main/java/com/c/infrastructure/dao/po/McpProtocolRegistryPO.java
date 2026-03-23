package com.c.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * MCP工具注册表：存储网关关联的工具接口配置
 *
 * @author cyh
 * @date 2026/03/23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpProtocolRegistryPO {

    /** 主键ID */
    private Long id;
    /** 所属网关ID */
    private String gatewayId;
    /** 工具ID */
    private Long toolId;
    /** MCP工具名称：如JavaSDKMCPClient_getCompanyEmployee */
    private String toolName;
    /** 工具类型：function/resource */
    private String toolType;
    /** 工具描述 */
    private String toolDescription;
    /** 工具版本 */
    private String toolVersion;
    /** HTTP接口地址 */
    private String httpUrl;
    /** HTTP请求方法：GET/POST/PUT/DELETE */
    private String httpMethod;
    /** HTTP请求头：JSON格式 */
    private String httpHeaders;
    /** 超时时间：接口调用超时阈值（毫秒） */
    private Integer timeout;
    /** 重试次数：接口调用失败后的重试次数 */
    private Integer retryTimes;
    /** 状态：0-禁用，1-启用 */
    private Integer status;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;

}