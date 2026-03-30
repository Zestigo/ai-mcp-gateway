package com.c.domain.admin.service.impl;

import com.c.domain.admin.adapter.repository.AdminRepository;
import com.c.domain.admin.model.entity.GatewayConfigEntity;
import com.c.domain.admin.service.AdminService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理员领域服务实现
 * 实现网关配置查询业务逻辑
 *
 * @author cyh
 * @date 2026/03/29
 */
@Service
public class AdminServiceImpl implements AdminService {

    /** 管理员仓储，负责数据层交互 */
    @Resource
    private AdminRepository adminRepository;

    /**
     * 查询网关配置列表
     *
     * @return 网关配置实体集合
     */
    @Override
    public List<GatewayConfigEntity> queryGatewayConfigList() {
        // 领域层直接调用仓储，不做逻辑处理，保持领域逻辑纯粹
        return adminRepository.queryGatewayConfigList();
    }

}