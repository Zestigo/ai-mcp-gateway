package com.c.domain.admin.model.entity;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import lombok.*;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Date;

/**
 * 鉴权领域实体 - 负责网关访问权限的核心业务逻辑
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayAuthEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态常量定义 */
    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;

    /** 密钥配置 */
    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int KEY_LENGTH = 64;

    /** 网关唯一标识 */
    private String gatewayId;

    /** API访问密钥 (由 Domain 内部生成) */
    private String apiKey;

    /** 访问速率限制 (QPH - Queries Per Hour) */
    private Integer rateLimit;

    /** 过期时间 */
    private Date expireTime;

    /** 权限状态 (0-禁用, 1-启用) */
    private Integer status;
    /** CAS乐观锁版本号 */
    private Long version;
    // ================= 核心领域行为 (Domain Behavior) =================

    /**
     * 初始化一个新的网关鉴权配置
     *
     * @param gatewayId 关联的网关ID
     */
    public void init(String gatewayId) {
        if (gatewayId == null || gatewayId
                .trim()
                .isEmpty()) {
            throw new IllegalArgumentException("网关ID不能为空");
        }
        this.gatewayId = gatewayId;
        this.status = STATUS_ENABLED; // 默认启用
        this.rateLimit = 1000;       // 默认每小时 1000 次，可根据业务调整
        generateFullSecureApiKey();
    }

    /**
     * 生成高强度 64 位 API Key
     */
    public void generateFullSecureApiKey() {
        this.apiKey = NanoIdUtils.randomNanoId(RANDOM, ALPHABET, KEY_LENGTH);
    }

    /**
     * 业务规则：判断当前配置是否有效
     * 结合了状态检查和过期时间检查
     */
    public boolean isValid() {
        if (this.status == STATUS_DISABLED) {
            return false;
        }
        if (this.expireTime != null && this.expireTime.before(new Date())) {
            return false;
        }
        return true;
    }

    /**
     * 禁用权限
     */
    public void disable() {
        this.status = STATUS_DISABLED;
    }

    /**
     * 启用权限
     */
    public void enable() {
        this.status = STATUS_ENABLED;
    }
}