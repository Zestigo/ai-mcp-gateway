package com.c.cases.mcp.session.node;

import com.c.cases.mcp.api.model.McpSessionRequest;
import com.c.cases.mcp.framework.tree.StrategyHandler;
import com.c.cases.mcp.session.AbstractMcpSessionSupport;
import com.c.cases.mcp.session.factory.DefaultMcpSessionFactory;
import com.c.domain.auth.model.entity.LicenseCommandEntity;
import com.c.domain.auth.service.AuthLicenseService;
import com.c.types.exception.AppException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 网关校验节点
 * 负责会话创建前的授权校验与网关配置校验
 * 是会话创建链路的准入控制节点
 *
 * @author cyh
 * @date 2026/03/28
 */
@Slf4j
@Service("mcpSessionVerifyNode")
public class VerifyNode extends AbstractMcpSessionSupport {

    /** 会话创建节点，校验通过后流转至此 */
    @Resource(name = "mcpSessionSessionNode")
    private SessionNode sessionNode;

    /** 授权认证领域服务，负责API密钥合法性校验 */
    @Resource
    private AuthLicenseService authLicenseService;

    /**
     * 执行网关与授权校验逻辑
     * 校验API密钥有效性，加载网关配置，填充上下文
     *
     * @param gatewayId 网关唯一标识
     * @param context   动态上下文
     * @return SSE事件流
     */
    @Override
    protected Flux<ServerSentEvent<String>> doApply(String gatewayId, DefaultMcpSessionFactory.DynamicContext context) {
        McpSessionRequest request = context.getSessionRequest();

        // 1. 构造授权校验命令
        LicenseCommandEntity commandEntity = LicenseCommandEntity
                .builder()
                .gatewayId(gatewayId)
                .apiKey(request.getApiKey())
                .build();

        log.info("[VerifyNode] 发起异步准入校验 | gatewayId: {}", gatewayId);

        // 2. 执行授权校验
        return authLicenseService
                .checkLicense(commandEntity)
                .flatMapMany(isValid -> {
                    // 授权无效，抛出业务异常
                    if (!Boolean.TRUE.equals(isValid)) {
                        log.warn("[VerifyNode] 授权校验未通过 | gatewayId: {}", gatewayId);
                        return Flux.error(new AppException("AUTH_001", "API Key 无效或授权已过期"));
                    }

                    // 3. 加载网关配置
                    var config = gatewayRepository.queryMcpGatewayConfigByGatewayId(gatewayId);
                    if (config == null) {
                        return Flux.error(new AppException("CONF_001", "未找到网关配置: " + gatewayId));
                    }

                    // 4. 填充配置到上下文
                    context.setGatewayConfigVO(config);
                    log.info("[VerifyNode] 准入校验通过，流转至下一节点");

                    // 5. 路由至会话创建节点
                    try {
                        return router(gatewayId, context);
                    } catch (Exception e) {
                        return Flux.error(e);
                    }
                });
    }

    /**
     * 获取责任链下一个执行节点
     * 校验通过后路由至会话创建节点
     *
     * @param gatewayId 网关唯一标识
     * @param context   动态上下文
     * @return 会话节点策略处理器
     */
    @Override
    public StrategyHandler<String, DefaultMcpSessionFactory.DynamicContext, Flux<ServerSentEvent<String>>> get(String gatewayId, DefaultMcpSessionFactory.DynamicContext context) {
        return sessionNode;
    }
}