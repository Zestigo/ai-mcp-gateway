package com.c.domain.auth.service;

import com.c.domain.auth.model.entity.RegisterCommandEntity;
import reactor.core.publisher.Mono;

/**
 * 网关注册服务接口
 * 定义网关认证信息注册标准能力，提供响应式注册接口
 *
 * @author cyh
 * @date 2026/03/27
 */
public interface AuthRegisterService {

    /**
     * 执行网关认证注册
     *
     * @param commandEntity 注册命令实体
     * @return 响应式结果，返回生成的API密钥
     */
    Mono<String> register(RegisterCommandEntity commandEntity);

}