package com.c.api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关协议配置传输对象
 * 用于封装网关HTTP协议相关的请求、超时、重试等配置信息
 *
 * @author cyh
 * @date 2026/03/31
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GatewayProtocolDTO {

    /** 所属网关协议ID，主键唯一标识 */
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

    /** CAS乐观锁版本号，用于并发更新控制 */
    private Long version;

}