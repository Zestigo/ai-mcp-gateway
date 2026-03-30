package com.c.domain.admin.service;

import com.c.domain.admin.model.entity.GatewayConfigEntity;

import java.util.List;

/**
 * 管理员领域服务接口
 * 定义网关配置业务逻辑规范
 *
 * @author cyh
 * @date 2026/03/29
 */
public interface AdminService {

    /**
     * 查询网关配置列表
     *
     * @return 网关配置实体集合
     */
    List<GatewayConfigEntity> queryGatewayConfigList();

}