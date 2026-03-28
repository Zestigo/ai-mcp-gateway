package com.c.domain.auth.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 限流校验命令实体
 * 用于封装网关接口限流校验所需的核心请求参数
 *
 * @author cyh
 * @date 2026/03/27
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RateLimitCommandEntity {

    /** 网关唯一标识 */
    private String gatewayId;

    /** 接口认证密钥 */
    private String apiKey;

}