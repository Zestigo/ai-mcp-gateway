package com.c.domain.gateway.service.gateway;

import com.c.domain.gateway.adapter.repository.GatewayRepository;
import com.c.domain.gateway.model.entity.GatewayConfigCommandEntity;
import com.c.domain.gateway.service.GatewayConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 网关配置服务实现类
 * 实现网关配置的保存、认证状态更新等业务逻辑
 *
 * @author cyh
 * @date 2026/03/29
 */
@Slf4j
@Service
public class GatewayConfigServiceImpl implements GatewayConfigService {

    @Resource
    private GatewayRepository gatewayRepository;

    /**
     * 保存网关配置
     *
     * @param commandEntity 网关配置命令实体
     */
    @Override
    public void saveGatewayConfig(GatewayConfigCommandEntity commandEntity) {
        gatewayRepository.saveGatewayConfig(commandEntity);
    }

    /**
     * 更新网关认证状态
     *
     * @param commandEntity 网关配置命令实体
     */
    @Override
    public void updateGatewayAuthStatus(GatewayConfigCommandEntity commandEntity) {
        gatewayRepository.updateGatewayAuthStatus(commandEntity);
    }
}