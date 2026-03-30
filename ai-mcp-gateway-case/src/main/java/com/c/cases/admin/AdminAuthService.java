package com.c.cases.admin;

import com.c.domain.auth.model.entity.RegisterCommandEntity;

/**
 * 认证配置管理服务接口
 * 定义网关认证相关操作
 *
 * @author cyh
 * @date 2026/03/29
 */
public interface AdminAuthService {

    /**
     * 保存网关认证配置
     *
     * @param commandEntity 注册命令实体
     * @return 生成的网关接入 API 密钥
     */
    String saveGatewayAuth(RegisterCommandEntity commandEntity);

}