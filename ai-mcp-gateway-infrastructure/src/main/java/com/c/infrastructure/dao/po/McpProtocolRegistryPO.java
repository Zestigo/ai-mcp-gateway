package com.c.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * MCP工具注册持久化对象
 * 对应数据库MCP工具注册表，用于存储网关关联的工具接口配置信息
 * 包含工具基础信息、HTTP调用配置、超时重试策略等核心内容
 *
 * @author cyh
 * @date 2026/03/25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpProtocolRegistryPO {

    /** 数据库自增主键ID */
    private Long id;

    /** 所属网关唯一标识，关联对应的网关配置 */
    private String gatewayId;

    /** 工具唯一标识，作为工具的业务主键 */
    private Long toolId;

    /** MCP工具名称，符合MCP协议规范的工具唯一名称 */
    private String toolName;

    /** 工具类型，支持function/resource两种标准MCP类型 */
    private String toolType;

    /** 工具描述，说明工具的功能、使用场景等信息 */
    private String toolDescription;

    /** 工具版本号，用于区分同一工具的不同版本 */
    private String toolVersion;

    /** 工具对应的HTTP接口请求地址 */
    private String httpUrl;

    /** HTTP请求方法，支持GET/POST/PUT/DELETE等标准方法 */
    private String httpMethod;

    /** HTTP请求头配置，以JSON格式存储多个请求头信息 */
    private String httpHeaders;

    /** 接口调用超时时间，单位为毫秒 */
    private Integer timeout;

    /** 接口调用失败后的最大重试次数 */
    private Integer retryTimes;

    /** 工具状态，0-禁用，1-启用，控制工具是否可调用 */
    private Integer status;

    /** 记录创建时间 */
    private Date createTime;

    /** 记录更新时间 */
    private Date updateTime;

}