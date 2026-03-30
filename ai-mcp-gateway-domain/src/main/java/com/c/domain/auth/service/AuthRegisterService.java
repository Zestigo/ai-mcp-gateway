package com.c.domain.auth.service;

import com.c.domain.auth.model.entity.RegisterCommandEntity;

/**
 * 网关注册服务接口
 * 定义网关认证授权注册的标准业务能力
 */
public interface AuthRegisterService {

    /**
     * 执行网关认证注册
     * 完成防重校验、密钥生成、授权信息持久化
     *
     * @param commandEntity 注册命令实体
     * @return 生成的网关接入 API 密钥
     */
    String register(RegisterCommandEntity commandEntity);

}