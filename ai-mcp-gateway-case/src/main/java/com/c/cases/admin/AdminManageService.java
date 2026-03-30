package com.c.cases.admin;

import com.c.domain.admin.model.entity.GatewayConfigEntity;

import java.util.List;

/**
 * 运营管理服务接口
 * 定义网关配置查询相关操作
 *
 * @author cyh
 * @date 2026/03/29
 */
public interface AdminManageService {

    /**
     * 查询网关配置列表
     *
     * @return 网关配置实体集合
     */
    List<GatewayConfigEntity> queryGatewayConfigList();

}