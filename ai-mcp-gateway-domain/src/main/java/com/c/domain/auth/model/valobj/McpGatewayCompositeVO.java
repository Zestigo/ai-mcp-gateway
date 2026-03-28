package com.c.domain.auth.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 网关聚合鉴权值对象
 * 封装网关主表与授权明细表全量鉴权信息，作为统一鉴权校验数据载体
 *
 * @author cyh
 * @date 2026/03/27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpGatewayCompositeVO {

    /** 网关唯一标识 */
    private String gatewayId;

    /** 网关全局鉴权开关，0-关闭鉴权直通模式，1-开启鉴权严格模式 */
    private Integer auth;

    /** 网关节点运行状态，0-停用维护中，1-正常运行 */
    private Integer gatewayStatus;

    /** 访问授权API密钥 */
    private String apiKey;

    /** API密钥授权状态，0-禁用，1-正常可用 */
    private Integer authStatus;

    /** API调用限流阈值 */
    private Long rateLimit;

    /** 授权过期时间 */
    private Date expireTime;

}