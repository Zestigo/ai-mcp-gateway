package com.c.cases.admin.manage;

import com.c.cases.admin.AdminManageService;
import com.c.domain.admin.model.entity.GatewayConfigEntity;
import com.c.domain.admin.service.AdminService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 运营管理服务实现
 * 提供网关配置查询能力
 *
 * @author cyh
 * @date 2026/03/29
 */
@Slf4j
@Service
public class AdminManageServiceImpl implements AdminManageService {

    /** 管理员领域服务，提供运营相关业务能力 */
    @Resource
    private AdminService adminService;

    /**
     * 查询网关配置列表
     *
     * @return 网关配置实体集合
     */
    @Override
    public List<GatewayConfigEntity> queryGatewayConfigList() {
        // 领域服务直接返回网关配置列表，应用层不做数据转换
        return adminService.queryGatewayConfigList();
    }

}