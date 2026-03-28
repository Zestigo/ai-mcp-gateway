package com.c.domain.auth.service;

import com.c.domain.auth.model.entity.LicenseCommandEntity;
import reactor.core.publisher.Mono;

/**
 * 认证证书校验服务接口
 * 定义网关证书合法性校验标准能力，提供响应式证书校验接口
 *
 * @author cyh
 * @date 2026/03/27
 */
public interface AuthLicenseService {

    /**
     * 校验网关证书合法性
     *
     * @param commandEntity 证书校验命令实体
     * @return 响应式校验结果，true为通过，false为失败
     */
    Mono<Boolean> checkLicense(LicenseCommandEntity commandEntity);

}