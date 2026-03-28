package com.c.domain.auth.service.license;

import com.c.domain.auth.adapter.repository.AuthRepository;
import com.c.domain.auth.model.entity.LicenseCommandEntity;
import com.c.domain.auth.model.valobj.enums.AuthStatusEnum;
import com.c.domain.auth.service.AuthLicenseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 权限证书服务实现类
 * 基于领域枚举实现语义化全链路鉴权校验，包含网关总闸、运行状态、API密钥、授权状态、有效期多层校验
 * 提供响应式鉴权校验核心能力，保障网关访问安全与权限控制
 *
 * @author cyh
 * @date 2026/03/27
 */
@Slf4j
@Service
public class AuthLicenseServiceImpl implements AuthLicenseService {

    /** 认证授权仓储接口，提供聚合认证信息查询能力 */
    @Resource
    private AuthRepository authRepository;

    /**
     * 执行网关证书全流程鉴权校验
     * 采用枚举语义化判断，依次校验网关模式、运行状态、API密钥合法性、授权状态、证书有效期
     * 包含空结果处理、超时熔断、异常捕获等安全防护机制
     *
     * @param commandEntity 证书校验命令实体，封装网关标识与API密钥信息
     * @return 响应式布尔结果，true表示鉴权通过，false表示鉴权失败
     */
    @Override
    public Mono<Boolean> checkLicense(LicenseCommandEntity commandEntity) {
        // 获取校验参数：网关唯一标识、API访问密钥
        String gatewayId = commandEntity.getGatewayId();
        String apiKey = commandEntity.getApiKey();

        return authRepository
                // 调用仓储层查询网关与密钥的聚合认证信息
                .queryCompositeAuth(gatewayId, apiKey)
                .map(vo -> {
                    // 第一层校验：网关总闸状态校验，非启用状态直接拦截请求
                    if (!AuthStatusEnum.GatewayConfig
                            .get(vo.getGatewayStatus())
                            .isEnabled()) {
                        log.warn("[鉴权拦截] 网关节点 {} 处于维护或停用状态", gatewayId);
                        return false;
                    }

                    // 第二层校验：网关授权策略校验，非强制校验模式直接放行请求
                    AuthStatusEnum.GatewayConfig authSwitch = AuthStatusEnum.GatewayConfig.get(vo.getAuth());
                    if (!authSwitch.isEnabled()) {
                        return true;
                    }

                    // 第三层校验：API密钥准入校验，无授权密钥记录判定为未授权访问
                    if (vo.getApiKey() == null) {
                        log.warn("[鉴权拦截] 未授权访问: Gateway={}, Key={}", gatewayId, apiKey);
                        return false;
                    }

                    // 第四层校验：API密钥状态校验，密钥禁用状态直接拦截请求
                    if (!AuthStatusEnum.AuthConfig
                            .get(vo.getAuthStatus())
                            .isEnable()) {
                        log.warn("[鉴权拦截] APIKey {} 已被禁用", apiKey);
                        return false;
                    }

                    // 第五层校验：证书有效期校验，返回时效校验结果
                    return validateExpireTime(vo.getExpireTime());
                })
                // 空结果处理：未查询到网关认证信息，判定为非法网关直接拦截
                .defaultIfEmpty(false)
                // 超时保护：300毫秒未响应则熔断，防止阻塞整体调用链
                .timeout(Duration.ofMillis(300))
                // 异常处理：系统异常时防御性返回失败，保障鉴权服务安全性
                .onErrorResume(e -> {
                    log.error("[鉴权系统异常] GatewayId: {}, 原因: {}", gatewayId, e.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * 校验证书有效期是否合法
     * 空值代表永久有效，非空则对比当前系统时间判断是否过期
     *
     * @param expireTime 证书过期时间戳对象
     * @return 校验结果布尔值，true表示证书有效，false表示证书已过期
     */
    private boolean validateExpireTime(java.util.Date expireTime) {
        // 过期时间为空代表证书永久有效
        if (expireTime == null) return true;
        // 系统当前时间小于过期时间，判定证书在有效期内
        return System.currentTimeMillis() < expireTime.getTime();
    }
}