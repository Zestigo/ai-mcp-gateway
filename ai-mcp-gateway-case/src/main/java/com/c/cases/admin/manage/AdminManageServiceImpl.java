package com.c.cases.admin.manage;

import com.c.cases.admin.AdminManageService;
import com.c.domain.admin.adapter.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 管理员管理服务实现类
 * 负责网关生命周期管理，包括发布、下线和配置同步等操作
 * 
 * @author cyh
 * @date 2026/03/31
 */
@Service
@RequiredArgsConstructor
public class AdminManageServiceImpl implements AdminManageService {

    /** 管理员仓库，提供网关管理的持久化操作 */
    private final AdminRepository adminRepository;

    /**
     * 发布网关
     * 
     * @param gatewayId 网关ID
     * @return 发布是否成功
     */
    @Override
    public boolean publishGateway(String gatewayId) {
        return adminRepository.publishGateway(gatewayId);
    }

    /**
     * 下线网关
     * 
     * @param gatewayId 网关ID
     * @return 下线是否成功
     */
    @Override
    public boolean offlineGateway(String gatewayId) {
        return adminRepository.offlineGateway(gatewayId);
    }

    /**
     * 同步网关配置
     * 
     * @param gatewayId 网关ID
     * @return 同步是否成功
     */
    @Override
    public boolean syncGatewayConfig(String gatewayId) {
        return adminRepository.syncGatewayConfig(gatewayId);
    }
}