package com.c.domain.gateway.service.tool;

import com.c.domain.gateway.adapter.repository.GatewayRepository;
import com.c.domain.gateway.model.entity.GatewayToolConfigCommandEntity;
import com.c.domain.gateway.service.GatewayToolConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 网关工具配置服务实现类
 * 实现网关工具配置的保存、协议更新等业务逻辑
 *
 * @author cyh
 * @date 2026/03/29
 */
@Slf4j
@Service
public class GatewayToolConfigServiceImpl implements GatewayToolConfigService {

    @Resource
    private GatewayRepository gatewayRepository;

    /**
     * 保存网关工具配置
     *
     * @param commandEntity 网关工具配置命令实体
     */
    @Override
    public void saveGatewayToolConfig(GatewayToolConfigCommandEntity commandEntity) {
        gatewayRepository.saveGatewayToolConfig(commandEntity);
    }

    /**
     * 更新网关工具协议配置
     *
     * @param commandEntity 网关工具配置命令实体
     */
    @Override
    public void updateGatewayToolProtocol(GatewayToolConfigCommandEntity commandEntity) {
        gatewayRepository.updateGatewayToolProtocol(commandEntity);
    }
}