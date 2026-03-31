package com.c.domain.admin.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayToolConfigEntity {
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

    private Integer toolStatus;

    /** 关联协议ID */
    private Long protocolId;

    /** 协议类型 */
    private String protocolType;
    /** CAS乐观锁版本号 */
    private Long  version;
}
