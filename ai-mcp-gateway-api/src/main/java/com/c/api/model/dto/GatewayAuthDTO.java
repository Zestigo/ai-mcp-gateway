package com.c.api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 网关认证信息传输对象
 * 用于封装网关身份认证、访问控制相关的核心配置数据
 *
 * @author cyh
 * @date 2026/03/31
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GatewayAuthDTO {

    /** 网关唯一标识，用于关联对应的网关配置 */
    private String gatewayId;

    /** API访问密钥，作为网关接口调用的身份凭证 */
    private String apiKey;

    /** 访问速率限制，配置每小时允许的最大访问次数 */
    private Integer rateLimit;

    /** API密钥过期时间，超过该时间密钥自动失效 */
    private Date expireTime;

    /** 权限状态，0-禁用，1-启用，控制网关权限是否生效 */
    private Integer status;

    /** CAS乐观锁版本号，用于并发更新控制 */
    private Long version;
}