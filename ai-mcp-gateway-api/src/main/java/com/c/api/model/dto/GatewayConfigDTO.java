package com.c.api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 网关基础配置信息传输对象
 * 用于封装网关的基础属性、运行状态、认证开关等核心配置
 *
 * @author cyh
 * @date 2026/03/31
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GatewayConfigDTO implements Serializable {

    /** 序列化ID，保证序列化与反序列化兼容性 */
    private static final long serialVersionUID = 1L;

    /** 网关唯一标识 */
    private String gatewayId;

    /** 网关名称 */
    private String gatewayName;

    /** 网关描述 */
    private String gatewayDesc;

    /** 网关业务版本号，格式示例：1.0.0/2.0.0 */
    private String gatewayVersion;

    /** 认证状态，0-关闭认证，1-开启认证 */
    private Integer auth;

    /** 网关运行状态，0-禁用，1-启用 */
    private Integer status;

    /** CAS乐观锁版本号，用于并发更新控制 */
    private Long version;

}