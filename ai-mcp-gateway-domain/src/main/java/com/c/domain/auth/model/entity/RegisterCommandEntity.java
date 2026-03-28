package com.c.domain.auth.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 网关注册命令实体
 * 用于封装网关注册/配置更新所需的业务参数
 *
 * @author cyh
 * @date 2026/03/27
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterCommandEntity {

    /** 网关唯一标识 */
    private String gatewayId;

    /** 接口调用速率限制，单位：次/小时 */
    private Integer rateLimit;

    /** 认证配置过期时间 */
    private Date expireTime;

}