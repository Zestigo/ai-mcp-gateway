package com.c.domain.admin.adapter.repository;

import com.c.domain.admin.model.entity.GatewayConfigEntity;

import java.util.List;

/**
 * 网关配置管理仓储接口
 * 提供网关配置相关的数据查询、操作能力定义
 *
 * @author cyh
 * @date 2026/03/30
 */
public interface AdminRepository {

    /**
     * 查询全量网关配置列表
     *
     * @return 网关配置实体集合
     */
    List<GatewayConfigEntity> queryGatewayConfigList();

}