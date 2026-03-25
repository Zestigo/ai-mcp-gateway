package com.c.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * MCP网关权限持久化对象
 * 对应数据库用户网关权限表，用于存储网关访问所需的API密钥、访问速率限制、密钥过期时间等权限控制配置信息
 *
 * @author cyh
 * @date 2026/03/25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpGatewayAuthPO {

    /** 数据库自增主键ID */
    private Long id;

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

    /** 记录创建时间 */
    private Date createTime;

    /** 记录更新时间 */
    private Date updateTime;

}