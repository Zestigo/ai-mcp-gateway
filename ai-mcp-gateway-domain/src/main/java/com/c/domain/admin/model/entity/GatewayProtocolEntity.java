package com.c.domain.admin.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayProtocolEntity {

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

    /** CAS乐观锁版本号 */
    private Long version;
}
