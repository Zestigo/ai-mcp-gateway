package com.c.cases.admin.auth;

import com.c.cases.admin.AdminAuthService;
import com.c.domain.admin.adapter.repository.AdminRepository;
import com.c.domain.admin.model.entity.GatewayAuthEntity;
import com.c.domain.admin.model.valobj.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * 管理后台网关认证服务实现类
 * 负责网关API密钥的新增、吊销、状态管理、查询等核心业务逻辑
 *
 * @author cyh
 * @date 2026/03/31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    /** 管理后台数据仓储接口，用于数据持久化操作 */
    private final AdminRepository adminRepository;

    /**
     * 保存网关认证信息
     *
     * @param entity 网关认证实体对象
     * @return 保存成功返回true，失败返回false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveAuth(GatewayAuthEntity entity) {
        Assert.notNull(entity, "鉴权实体不能为空");
        Assert.hasText(entity.getGatewayId(), "网关ID不能为空");
        Assert.notNull(entity.getStatus(), "状态不能为空");
        return adminRepository.saveAuth(entity);
    }

    /**
     * 吊销网关API密钥
     *
     * @param gatewayId 网关唯一标识
     * @param apiKey    API访问密钥
     * @return 吊销成功返回true，失败返回false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean revokeApiKey(String gatewayId, String apiKey) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        Assert.hasText(apiKey, "apiKey不能为空");
        return adminRepository.revokeApiKey(gatewayId, apiKey);
    }

    /**
     * 启用网关API密钥（带CAS乐观锁）
     *
     * @param gatewayId  网关唯一标识
     * @param apiKey     API访问密钥
     * @param oldVersion 乐观锁旧版本号
     * @return 启用成功返回true，失败返回false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enableApiKey(String gatewayId, String apiKey, Long oldVersion) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        Assert.hasText(apiKey, "apiKey不能为空");
        Assert.notNull(oldVersion, "乐观锁版本号不能为空");
        return adminRepository.updateApiKeyStatusByCas(gatewayId, apiKey, 1, oldVersion);
    }

    /**
     * 禁用网关API密钥（带CAS乐观锁）
     *
     * @param gatewayId  网关唯一标识
     * @param apiKey     API访问密钥
     * @param oldVersion 乐观锁旧版本号
     * @return 禁用成功返回true，失败返回false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableApiKey(String gatewayId, String apiKey, Long oldVersion) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        Assert.hasText(apiKey, "apiKey不能为空");
        Assert.notNull(oldVersion, "乐观锁版本号不能为空");
        return adminRepository.updateApiKeyStatusByCas(gatewayId, apiKey, 0, oldVersion);
    }

    /**
     * 分页查询网关认证信息列表
     *
     * @param pageNo    当前页码
     * @param pageSize  每页条数
     * @param gatewayId 网关唯一标识
     * @return 分页网关认证数据
     */
    @Override
    public PageResponse<GatewayAuthEntity> queryGatewayAuthPage(int pageNo, int pageSize, String gatewayId) {
        return adminRepository.queryGatewayAuthPage(pageNo, pageSize, gatewayId);
    }

    /**
     * 根据API密钥查询认证详情
     *
     * @param apiKey API访问密钥
     * @return 网关认证实体对象
     */
    @Override
    public GatewayAuthEntity findAuthByApiKey(String apiKey) {
        Assert.hasText(apiKey, "apiKey不能为空");
        return adminRepository.findAuthByApiKey(apiKey);
    }

    /**
     * 判断API密钥是否已存在
     *
     * @param apiKey API访问密钥
     * @return 存在返回true，不存在返回false
     */
    @Override
    public boolean isApiKeyExists(String apiKey) {
        Assert.hasText(apiKey, "apiKey不能为空");
        return adminRepository.isApiKeyExists(apiKey);
    }
}