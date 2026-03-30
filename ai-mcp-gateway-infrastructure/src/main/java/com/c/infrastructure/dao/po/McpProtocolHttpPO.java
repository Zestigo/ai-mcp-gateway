package com.c.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * MCP协议HTTP配置持久化对象
 * 用于存储MCP工具HTTP调用相关的配置信息，包括接口地址、请求方式、超时、重试等配置
 *
 * @author cyh
 * @date 2026/03/29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpProtocolHttpPO {

    /** 主键ID */
    private Long id;

    /** 所属网关协议ID */
    private Long protocolId;

    /** HTTP接口请求地址 */
    private String httpUrl;

    /** HTTP请求方法，可选值：GET、POST、PUT、DELETE */
    private String httpMethod;

    /** HTTP请求头，以JSON字符串格式存储 */
    private String httpHeaders;

    /** HTTP请求超时时间，单位：毫秒 */
    private Integer timeout;

    /** HTTP请求失败重试次数 */
    private Integer retryTimes;

    /** 配置状态，0-禁用，1-启用 */
    private Integer status;

    /** 记录创建时间 */
    private Date createTime;

    /** 记录更新时间 */
    private Date updateTime;

}