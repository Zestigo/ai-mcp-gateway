package com.c.domain.auth.service.register;

import com.c.domain.auth.adapter.repository.AuthRepository;
import com.c.domain.auth.model.entity.RegisterCommandEntity;
import com.c.domain.auth.model.valobj.McpGatewayAuthVO;
import com.c.domain.auth.model.valobj.enums.AuthStatusEnum;
import com.c.domain.auth.service.AuthRegisterService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 权限注册服务实现类
 * 提供网关节点授权注册能力，包含防重校验、安全密钥生成、授权信息持久化全流程业务处理
 *
 * @author cyh
 * @date 2026/03/27
 */
@Slf4j
@Service
public class AuthRegisterServiceImpl implements AuthRegisterService {

    /** 认证授权仓储接口，提供注册信息查询与持久化能力 */
    @Resource
    private AuthRepository repository;

    /**
     * 执行网关节点授权注册
     * 流程：重复注册校验 -> 生成安全API密钥 -> 构建授权对象 -> 持久化存储 -> 返回密钥
     *
     * @param commandEntity 注册命令实体，封装网关标识、限流值、过期时间信息
     * @return 响应式字符串结果，注册成功返回分配的API密钥
     * @throws RuntimeException 网关已存在有效授权时抛出业务异常
     */
    @Override
    public Mono<String> register(RegisterCommandEntity commandEntity) {
        // 获取注册核心参数：网关唯一标识
        String gatewayId = commandEntity.getGatewayId();

        return Mono
                .fromCallable(() -> {
                    // 1. 业务防重校验：查询网关是否已存在有效授权记录
                    int existingCount = repository.queryEffectiveGatewayAuthCount(gatewayId);
                    if (existingCount > 0) {
                        log.warn("[注册拦截] 网关节点 {} 已存在有效授权，禁止重复注册", gatewayId);
                        throw new RuntimeException("Gateway already registered");
                    }

                    // 2. 按规范生成安全API访问密钥
                    String apiKey = generateSecureApiKey();

                    // 3. 构建授权值对象，封装注册信息与默认启用状态
                    McpGatewayAuthVO vo = McpGatewayAuthVO
                            .builder()
                            .gatewayId(gatewayId)
                            .apiKey(apiKey)
                            .rateLimit(commandEntity.getRateLimit())
                            .expireTime(commandEntity.getExpireTime())
                            .status(AuthStatusEnum.AuthConfig.ENABLE)
                            .build();

                    // 4. 持久化授权信息，仓储层自动处理数据库写入与缓存清理
                    repository.insert(vo);

                    log.info("[算力节点注册] 成功为网关 {} 分配接入凭证", gatewayId);
                    return apiKey;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    // 注册异常日志记录，向上层传递业务异常
                    log.error("[注册失败] 网关: {}, 原因: {}", gatewayId, e.getMessage());
                    return Mono.error(e);
                });
    }

    /**
     * 生成符合网关安全规范的API密钥
     * 密钥格式：固定前缀gw_ + 62位随机字母数字组合
     *
     * @return 生成的安全API密钥字符串
     */
    private String generateSecureApiKey() {
        return "gw_" + RandomStringUtils.randomAlphanumeric(62);
    }
}