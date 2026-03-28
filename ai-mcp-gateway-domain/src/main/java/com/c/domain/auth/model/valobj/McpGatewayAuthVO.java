package com.c.domain.auth.model.valobj;

import com.c.domain.auth.model.valobj.enums.AuthStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 网关认证值对象
 * 封装网关认证相关的完整配置与状态信息
 *
 * @author cyh
 * @date 2026/03/27
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class McpGatewayAuthVO {

    /** 网关唯一标识 */
    private String gatewayId;

    /** 接口认证密钥 */
    private String apiKey;

    /** 接口调用速率限制，单位：次/小时 */
    private Integer rateLimit;

    /** 认证配置过期时间 */
    private Date expireTime;

    /** 认证状态：0-禁用，1-启用 */
    private AuthStatusEnum.AuthConfig status;

}