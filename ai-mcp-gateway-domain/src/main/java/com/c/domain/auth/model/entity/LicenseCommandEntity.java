package com.c.domain.auth.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 证书校验命令实体
 * 用于封装网关证书校验所需的核心请求参数
 *
 * @author cyh
 * @date 2026/03/27
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LicenseCommandEntity {

    /** 网关唯一标识 */
    private String gatewayId;

    /** 接口认证密钥 */
    private String apiKey;

}