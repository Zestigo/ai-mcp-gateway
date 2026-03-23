package com.c.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 用户网关权限表：存储网关访问的API密钥、速率限制等权限配置
 *
 * @author cyh
 * @date 2026/03/23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpGatewayAuthPO {

    /** 主键ID */
    private Long id;
    /** 网关唯一标识 */
    private String gatewayId;
    /** API密钥：网关访问凭证 */
    private String apiKey;
    /** 速率限制：每小时允许的访问次数 */
    private Integer rateLimit;
    /** 过期时间：API密钥失效时间 */
    private Date expireTime;
    /** 状态：0-禁用，1-启用 */
    private Integer status;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;

}