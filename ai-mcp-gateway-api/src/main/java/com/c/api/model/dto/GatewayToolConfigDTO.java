package com.c.api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关工具配置传输对象
 * 用于封装网关与MCP工具关联的配置、协议绑定、状态等信息
 *
 * @author cyh
 * @date 2026/03/31
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GatewayToolConfigDTO {

    /** 所属网关唯一标识 */
    private String gatewayId;

    /** 工具唯一标识 */
    private Long toolId;

    /** MCP工具名称 */
    private String toolName;

    /** 工具类型 */
    private String toolType;

    /** 工具描述信息 */
    private String toolDescription;

    /** 工具版本号 */
    private String toolVersion;

    /** 工具状态，0-禁用，1-启用 */
    private Integer status;

    /** 关联协议ID */
    private Long protocolId;

    /** 协议类型 */
    private String protocolType;

    /** CAS乐观锁版本号，用于并发更新控制 */
    private Long version;
}