package com.c.domain.auth.model.valobj;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.c.domain.auth.model.valobj.enums.AuthStatusEnum;
import lombok.*;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Date;

/**
 * 网关认证值对象 - 增强版
 * 具备自生成 ApiKey 能力，确保业务链条不中断
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class McpGatewayAuthVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 静态常量：生成 64 位 Key 的配置 */
    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEFAULT_KEY_LENGTH = 64;

    private String gatewayId;
    private String apiKey;
    private Integer rateLimit;
    private Date expireTime;
    private AuthStatusEnum.AuthConfig status;
    public String generateKey() {
        if (this.apiKey == null || this.apiKey.isEmpty()) {
            this.apiKey = NanoIdUtils.randomNanoId(RANDOM, ALPHABET, 64);
        }
        return null;
    }
}