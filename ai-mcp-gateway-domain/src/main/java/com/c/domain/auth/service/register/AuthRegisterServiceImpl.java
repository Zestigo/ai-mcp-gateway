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

/**
 * 网关注册服务实现
 * 负责网关认证授权的核心业务逻辑：防重注册、密钥生成、持久化
 *
 * @author cyh
 * @date 2026/03/30
 */
@Slf4j
@Service
public class AuthRegisterServiceImpl implements AuthRegisterService {

    /**
     * 认证仓储接口
     * 负责网关认证数据的持久化操作
     */
    @Resource
    private AuthRepository repository;

    /**
     * 执行网关注册逻辑
     * 1. 校验是否已存在有效授权
     * 2. 生成安全唯一的 API Key
     * 3. 构建认证信息并持久化
     *
     * @param commandEntity 注册命令实体（携带网关ID、限流、过期时间等）
     * @return 生成的网关接入凭证 API Key
     */
    @Override
    public String register(RegisterCommandEntity commandEntity) {
        // 获取网关唯一标识
        String gatewayId = commandEntity.getGatewayId();

        // 1. 业务防重校验：同一网关不允许重复注册有效授权
        int existingCount = repository.queryEffectiveGatewayAuthCount(gatewayId);
        if (existingCount > 0) {
            log.warn("[注册拦截] 网关节点 {} 已存在有效授权", gatewayId);
            throw new RuntimeException("Gateway already registered");
        }

        // 2. 生成安全的网关接入密钥
        String apiKey = generateSecureApiKey();

        // 3. 构建认证值对象，设置注册信息与启用状态
        McpGatewayAuthVO vo = McpGatewayAuthVO
                .builder()
                .gatewayId(gatewayId)
                .apiKey(apiKey)
                .rateLimit(commandEntity.getRateLimit())
                .expireTime(commandEntity.getExpireTime())
                .status(AuthStatusEnum.AuthConfig.ENABLE)
                .build();

        try {
            // 持久化认证信息
            repository.insert(vo);
            log.info("[算力节点注册] 成功为网关 {} 分配接入凭证", gatewayId);
            return apiKey;
        } catch (Exception e) {
            // 注册异常，打印日志并抛出，保证事务回滚
            log.error("[注册失败] 网关: {}, 原因: {}", gatewayId, e.getMessage());
            throw e;
        }
    }

    /**
     * 生成安全、唯一的网关 API Key
     * 前缀 gw_ + 62 位随机字母数字，保证安全性与可读性
     *
     * @return 生成的 API Key
     */
    private String generateSecureApiKey() {
        return "gw_" + RandomStringUtils.randomAlphanumeric(62);
    }
}