package com.c.cases.admin.auth;

import com.c.cases.admin.AdminAuthService;
import com.c.domain.auth.model.entity.RegisterCommandEntity;
import com.c.domain.auth.service.AuthRegisterService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 管理员认证配置服务实现
 * 应用层服务，承接上层接口请求，调用领域服务完成认证配置保存
 *
 * @author cyh
 * @date 2026/03/30
 */
@Slf4j
@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    /**
     * 网关注册领域服务
     * 提供认证授权核心业务能力
     */
    @Resource
    private AuthRegisterService authRegisterService;

    /**
     * 保存网关认证配置
     * 委托领域服务完成注册逻辑
     *
     * @param commandEntity 认证配置命令实体
     * @return 生成的网关接入 API 密钥
     */
    @Override
    public String saveGatewayAuth(RegisterCommandEntity commandEntity) {
        // 应用层仅做转发，核心注册逻辑由领域层实现
        return authRegisterService.register(commandEntity);
    }

}