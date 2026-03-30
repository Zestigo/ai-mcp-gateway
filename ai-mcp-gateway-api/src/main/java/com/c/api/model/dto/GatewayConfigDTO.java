package com.c.api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 网关配置信息DTO
 *
 * @author cyh
 * @date 2026/03/29
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GatewayConfigDTO implements Serializable {

    /** 网关唯一标识 */
    private String gatewayId;

    /** 网关名称 */
    private String gatewayName;

    /** 网关描述 */
    private String gatewayDesc;

    /** 版本号 */
    private String version;

    /** 认证状态 */
    private Integer auth;

    /** 网关状态 */
    private Integer status;

}