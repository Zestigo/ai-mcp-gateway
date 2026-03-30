package com.c.cases.admin.gateway;

import com.c.cases.admin.AdminGatewayService;
import com.c.domain.gateway.model.entity.GatewayConfigCommandEntity;
import com.c.domain.gateway.model.entity.GatewayToolConfigCommandEntity;
import com.c.domain.gateway.service.GatewayConfigService;
import com.c.domain.gateway.service.GatewayToolConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 网关配置管理服务实现
 * 处理网关基础配置与工具配置的保存逻辑
 *
 * @author cyh
 * @date 2026/03/29
 */
@Slf4j
@Service
public class AdminGatewayServiceImpl implements AdminGatewayService {

    /** 网关配置领域服务，处理网关核心配置业务逻辑 */
    @Resource
    private GatewayConfigService gatewayConfigService;

    /** 网关工具配置领域服务，处理网关工具链配置业务逻辑 */
    @Resource
    private GatewayToolConfigService gatewayToolConfigService;

    /**
     * 保存网关基础配置
     *
     * @param commandEntity 网关配置命令实体
     */
    @Override
    public void saveGatewayConfig(GatewayConfigCommandEntity commandEntity) {
        // 直接调用领域服务完成网关配置保存，不做业务逻辑处理
        gatewayConfigService.saveGatewayConfig(commandEntity);
    }

    /**
     * 保存网关工具配置
     *
     * @param commandEntity 网关工具配置命令实体
     */
    @Override
    public void saveGatewayToolConfig(GatewayToolConfigCommandEntity commandEntity) {
        // 调用工具配置领域服务，完成工具链配置持久化
        gatewayToolConfigService.saveGatewayToolConfig(commandEntity);
    }

}