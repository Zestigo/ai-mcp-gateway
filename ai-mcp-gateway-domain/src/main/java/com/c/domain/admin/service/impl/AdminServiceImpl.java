package com.c.domain.admin.service.impl;

import com.c.domain.admin.adapter.repository.AdminRepository;
import com.c.domain.admin.model.entity.*;
import com.c.domain.admin.model.valobj.PageResponse;
import com.c.domain.admin.service.AdminService;
import com.c.types.enums.ResponseCode;
import com.c.types.exception.AppException;
import com.c.types.utils.BizAssert;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * 管理后台领域服务实现类
 * 实现网关、工具、协议、鉴权的核心业务逻辑，包含参数校验、分页修正、CAS乐观锁控制
 *
 * @author cyh
 * @date 2026/03/31
 */
@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    /** 数据仓储接口，用于数据持久化操作 */
    @Resource
    private AdminRepository adminRepository;

    /** 默认访问速率限制值 */
    private static final int DEFAULT_RATE_LIMIT = 100;
    /** 安全随机数生成器，用于生成API密钥 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /** API密钥长度 */
    private static final int API_KEY_LENGTH = 32;
    /** 最大分页条数限制 */
    private static final int MAX_PAGE_SIZE = 100;
    /** 启用状态常量 */
    private static final Integer ENABLE = 1;
    /** 禁用状态常量 */
    private static final Integer DISABLE = 0;

    // ==================== 网关 ====================

    /**
     * 分页查询网关配置列表，自动修正分页参数
     *
     * @param pageNo   当前页码
     * @param pageSize 每页条数
     * @param keyword  搜索关键词
     * @param status   网关状态
     * @return 分页网关配置数据
     */
    @Override
    public PageResponse<GatewayConfigEntity> queryGatewayConfigPage(int pageNo, int pageSize, String keyword,
                                                                    Integer status) {
        pageNo = Math.max(pageNo, 1);
        pageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        return adminRepository.queryGatewayConfigPage(pageNo, pageSize, keyword, status);
    }

    /**
     * 根据网关ID查询网关详情，不存在则抛出异常
     *
     * @param gatewayId 网关唯一标识
     * @return 网关配置实体
     */
    @Override
    public GatewayConfigEntity findGatewayById(String gatewayId) {
        BizAssert.notBlank(gatewayId, "网关ID不能为空");
        return Optional
                .ofNullable(adminRepository.findGatewayById(gatewayId))
                .orElseThrow(() -> new AppException(ResponseCode.DATA_NOT_FOUND.getCode(), "网关不存在"));
    }

    /**
     * 创建网关配置，参数非空校验
     *
     * @param entity 网关配置实体
     * @return 创建结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createGateway(GatewayConfigEntity entity) {
        BizAssert.notNull(entity, "网关配置不能为空");
        BizAssert.notBlank(entity.getGatewayId(), "网关ID不能为空");
        return adminRepository.createGateway(entity);
    }

    /**
     * 更新网关配置，带CAS乐观锁，自动校验网关是否存在
     *
     * @param entity     网关配置实体
     * @param oldVersion 旧版本号
     * @return 更新结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateGateway(GatewayConfigEntity entity, String oldVersion) {
        BizAssert.notNull(entity, "配置不能为空");
        BizAssert.notBlank(entity.getGatewayId(), "网关ID不能为空");
        BizAssert.notBlank(oldVersion, "版本号不能为空");

        findGatewayById(entity.getGatewayId());
        return adminRepository.updateGatewayByCas(entity, Long.parseLong(oldVersion));
    }

    /**
     * 启用网关，带CAS乐观锁
     *
     * @param gatewayId  网关唯一标识
     * @param oldVersion 旧版本号
     * @return 启用结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enableGateway(String gatewayId, String oldVersion) {
        BizAssert.notBlank(gatewayId, "网关ID不能为空");
        BizAssert.notBlank(oldVersion, "版本号不能为空");
        return adminRepository.updateGatewayStatusByCas(gatewayId, Long.parseLong(oldVersion), ENABLE);
    }

    /**
     * 禁用网关，带CAS乐观锁
     *
     * @param gatewayId  网关唯一标识
     * @param oldVersion 旧版本号
     * @return 禁用结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableGateway(String gatewayId, String oldVersion) {
        BizAssert.notBlank(gatewayId, "网关ID不能为空");
        BizAssert.notBlank(oldVersion, "版本号不能为空");
        return adminRepository.updateGatewayStatusByCas(gatewayId, Long.parseLong(oldVersion), DISABLE);
    }

    /**
     * 删除网关，级联吊销API密钥
     *
     * @param gatewayId 网关唯一标识
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteGateway(String gatewayId) {
        BizAssert.notBlank(gatewayId, "网关ID不能为空");
        adminRepository.revokeApiKey(gatewayId, null);
        return adminRepository.deleteGateway(gatewayId);
    }

    // ==================== 工具 ====================

    /**
     * 分页查询工具配置，自动修正分页参数
     *
     * @param pageNo     当前页码
     * @param pageSize   每页条数
     * @param gatewayId  网关标识
     * @param protocolId 协议标识
     * @param toolStatus 工具状态
     * @return 分页工具配置数据
     */
    @Override
    public PageResponse<GatewayToolConfigEntity> queryGatewayToolConfigPage(int pageNo, int pageSize,
                                                                            String gatewayId, Long protocolId,
                                                                            Integer toolStatus) {
        pageNo = Math.max(pageNo, 1);
        pageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        return adminRepository.queryToolPage(pageNo, pageSize, gatewayId, protocolId, toolStatus);
    }

    /**
     * 根据网关ID查询所有绑定工具
     *
     * @param gatewayId 网关唯一标识
     * @return 工具配置列表
     */
    @Override
    public List<GatewayToolConfigEntity> getToolsByGatewayId(String gatewayId) {
        BizAssert.notBlank(gatewayId, "网关ID不能为空");
        return adminRepository.listToolsByGateway(gatewayId);
    }

    /**
     * 根据三主键查询唯一工具配置
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @return 工具配置实体
     */
    @Override
    public GatewayToolConfigEntity getToolConfigUnique(String gatewayId, Long toolId, Long protocolId) {
        BizAssert.notBlank(gatewayId, "网关ID不能为空");
        BizAssert.notNull(toolId, "工具ID不能为空");
        BizAssert.notNull(protocolId, "协议ID不能为空");

        return adminRepository
                .listToolsByGateway(gatewayId)
                .stream()
                .filter(t -> toolId.equals(t.getToolId()) && protocolId.equals(t.getProtocolId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 绑定工具到网关，校验重复绑定
     *
     * @param entity 工具配置实体
     * @return 绑定结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addToolConfig(GatewayToolConfigEntity entity) {
        BizAssert.notNull(entity, "工具配置不能为空");
        BizAssert.notBlank(entity.getGatewayId(), "网关ID不能为空");
        BizAssert.notNull(entity.getToolId(), "工具ID不能为空");
        BizAssert.notNull(entity.getProtocolId(), "协议ID不能为空");
        BizAssert.isTrue(!isToolConfigured(entity.getGatewayId(), entity.getToolId(), entity.getProtocolId()), "工具已绑定");

        return adminRepository.bindTool(entity);
    }

    /**
     * 更新工具配置，带CAS乐观锁
     *
     * @param entity     工具配置实体
     * @param oldVersion 旧版本号
     * @return 更新结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateToolConfig(GatewayToolConfigEntity entity, String oldVersion) {
        BizAssert.notNull(entity, "配置不能为空");
        BizAssert.notBlank(entity.getGatewayId(), "网关ID不能为空");
        BizAssert.notNull(entity.getToolId(), "工具ID不能为空");
        BizAssert.notNull(entity.getProtocolId(), "协议ID不能为空");
        BizAssert.notBlank(oldVersion, "版本号不能为空");

        return adminRepository.updateToolConfigByCas(entity, Long.parseLong(oldVersion));
    }

    /**
     * 启用工具，带CAS乐观锁
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @param oldVersion 旧版本号
     * @return 启用结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enableTool(String gatewayId, Long toolId, Long protocolId, String oldVersion) {
        BizAssert.notBlank(gatewayId, "网关ID不能为空");
        BizAssert.notNull(toolId, "工具ID不能为空");
        BizAssert.notNull(protocolId, "协议ID不能为空");
        BizAssert.notBlank(oldVersion, "版本号不能为空");

        return adminRepository.updateToolStatusByCas(gatewayId, Math.toIntExact(toolId), protocolId, ENABLE,
                Long.parseLong(oldVersion));
    }

    /**
     * 禁用工具，带CAS乐观锁
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @param oldVersion 旧版本号
     * @return 禁用结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableTool(String gatewayId, Long toolId, Long protocolId, String oldVersion) {
        BizAssert.notBlank(gatewayId, "网关ID不能为空");
        BizAssert.notNull(toolId, "工具ID不能为空");
        BizAssert.notNull(protocolId, "协议ID不能为空");
        BizAssert.notBlank(oldVersion, "版本号不能为空");

        return adminRepository.updateToolStatusByCas(gatewayId, Math.toIntExact(toolId), protocolId, DISABLE,
                Long.parseLong(oldVersion));
    }

    /**
     * 解绑网关与工具
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @return 解绑结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeToolFromGateway(String gatewayId, Long toolId, Long protocolId) {
        BizAssert.notBlank(gatewayId, "网关ID不能为空");
        BizAssert.notNull(toolId, "工具ID不能为空");
        BizAssert.notNull(protocolId, "协议ID不能为空");
        return adminRepository.unbindTool(gatewayId, Math.toIntExact(toolId), protocolId);
    }

    /**
     * 校验工具是否已绑定
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @return 校验结果
     */
    @Override
    public boolean isToolConfigured(String gatewayId, Long toolId, Long protocolId) {
        BizAssert.notBlank(gatewayId, "网关ID不能为空");
        BizAssert.notNull(toolId, "工具ID不能为空");
        BizAssert.notNull(protocolId, "协议ID不能为空");
        return adminRepository.isToolExist(gatewayId, Math.toIntExact(toolId), protocolId);
    }

    // ==================== 协议 ====================

    /**
     * 分页查询协议配置，自动修正分页参数
     *
     * @param pageNo     当前页码
     * @param pageSize   每页条数
     * @param protocolId 协议ID
     * @param status     协议状态
     * @return 分页协议配置数据
     */
    @Override
    public PageResponse<GatewayProtocolEntity> queryProtocolConfigPage(int pageNo, int pageSize, Long protocolId,
                                                                       Integer status) {
        pageNo = Math.max(pageNo, 1);
        pageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        return adminRepository.queryProtocolConfigPage(pageNo, pageSize, protocolId, status);
    }

    /**
     * 查询协议配置列表
     *
     * @param protocolId 协议ID
     * @param status     协议状态
     * @return 协议配置列表
     */
    @Override
    public List<GatewayProtocolEntity> getProtocolConfigList(Long protocolId, Integer status) {
        return adminRepository.getProtocolConfigList(protocolId, status);
    }

    /**
     * 根据协议ID查询协议详情，不存在则抛出异常
     *
     * @param id 协议唯一标识
     * @return 协议配置实体
     */
    @Override
    public GatewayProtocolEntity findProtocolById(Long id) {
        BizAssert.notNull(id, "协议ID不能为空");
        return Optional
                .ofNullable(adminRepository.findByProtocolId(id))
                .orElseThrow(() -> new AppException(ResponseCode.DATA_NOT_FOUND.getCode(), "协议不存在"));
    }

    /**
     * 创建协议配置
     *
     * @param entity 协议配置实体
     * @return 创建结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createProtocol(GatewayProtocolEntity entity) {
        BizAssert.notNull(entity, "协议不能为空");
        return adminRepository.createProtocol(entity);
    }

    /**
     * 更新协议配置，带CAS乐观锁
     *
     * @param entity     协议配置实体
     * @param oldVersion 旧版本号
     * @return 更新结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProtocolConfig(GatewayProtocolEntity entity, String oldVersion) {
        BizAssert.notNull(entity, "协议不能为空");
        BizAssert.notBlank(oldVersion, "版本号不能为空");
        return adminRepository.updateProtocolConfigByCas(entity, Long.parseLong(oldVersion));
    }

    /**
     * 根据协议ID删除协议
     *
     * @param id 协议唯一标识
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteProtocolById(Long id) {
        BizAssert.notNull(id, "协议ID不能为空");
        return adminRepository.deleteProtocolById(id);
    }

    /**
     * 根据协议组ID删除协议配置
     *
     * @param protocolId 协议组ID
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteProtocolsByPId(Long protocolId) {
        BizAssert.notNull(protocolId, "协议组ID不能为空");
        return adminRepository.deleteProtocolById(protocolId);
    }

    // ==================== 鉴权 ====================

    /**
     * 分页查询API密钥，自动修正分页参数
     *
     * @param pageNo    当前页码
     * @param pageSize  每页条数
     * @param gatewayId 网关唯一标识
     * @return 分页认证数据
     */
    @Override
    public PageResponse<GatewayAuthEntity> queryGatewayAuthPage(int pageNo, int pageSize, String gatewayId) {
        pageNo = Math.max(pageNo, 1);
        pageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        return adminRepository.queryGatewayAuthPage(pageNo, pageSize, gatewayId);
    }

    /**
     * 根据网关ID查询所有API密钥
     *
     * @param gatewayId 网关唯一标识
     * @return 认证信息列表
     */
    @Override
    public List<GatewayAuthEntity> getAuthListByGatewayId(String gatewayId) {
        BizAssert.notBlank(gatewayId, "网关ID不能为空");
        return adminRepository.getAuthListByGatewayId(gatewayId);
    }

    /**
     * 根据API密钥查询详情，不存在则抛出异常
     *
     * @param apiKey API访问密钥
     * @return 认证实体
     */
    @Override
    public GatewayAuthEntity findAuthByApiKey(String apiKey) {
        BizAssert.notBlank(apiKey, "API Key不能为空");
        return Optional
                .ofNullable(adminRepository.findAuthByApiKey(apiKey))
                .orElseThrow(() -> new AppException(ResponseCode.DATA_NOT_FOUND.getCode(), "API Key不存在"));
    }

    /**
     * 校验API密钥是否存在
     *
     * @param apiKey API访问密钥
     * @return 校验结果
     */
    @Override
    public boolean isApiKeyExists(String apiKey) {
        BizAssert.notBlank(apiKey, "API Key不能为空");
        return adminRepository.isApiKeyExists(apiKey);
    }

    /**
     * 保存认证信息
     *
     * @param entity 认证实体
     * @return 保存结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveAuth(GatewayAuthEntity entity) {
        BizAssert.notNull(entity, "鉴权信息不能为空");
        BizAssert.notBlank(entity.getGatewayId(), "网关ID不能为空");
        BizAssert.notBlank(entity.getApiKey(), "API Key不能为空");
        return adminRepository.saveAuth(entity);
    }

    /**
     * 生成安全API密钥并保存，自动处理冲突
     *
     * @param gatewayId 网关唯一标识
     * @param rateLimit 访问速率限制
     * @return 生成的API密钥
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateAndSaveApiKey(String gatewayId, Integer rateLimit) {
        BizAssert.notBlank(gatewayId, "网关ID不能为空");
        int limit = Optional
                .ofNullable(rateLimit)
                .orElse(DEFAULT_RATE_LIMIT);

        byte[] bytes = new byte[API_KEY_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        String apiKey = Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        if (isApiKeyExists(apiKey)) {
            log.warn("API Key冲突，重新生成 gatewayId:{}", gatewayId);
            return generateAndSaveApiKey(gatewayId, rateLimit);
        }

        GatewayAuthEntity auth = GatewayAuthEntity
                .builder()
                .gatewayId(gatewayId)
                .apiKey(apiKey)
                .rateLimit(limit)
                .status(ENABLE)
                .build();

        saveAuth(auth);
        log.info("生成API Key gatewayId:{}", gatewayId);
        return apiKey;
    }

    /**
     * 启用API密钥，带CAS乐观锁
     *
     * @param gatewayId  网关标识
     * @param apiKey     API密钥
     * @param oldVersion 旧版本号
     * @return 启用结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enableApiKey(String gatewayId, String apiKey, String oldVersion) {
        BizAssert.notBlank(gatewayId, "网关ID不能为空");
        BizAssert.notBlank(apiKey, "API Key不能为空");
        BizAssert.notBlank(oldVersion, "版本号不能为空");
        return adminRepository.updateApiKeyStatusByCas(gatewayId, apiKey, ENABLE, Long.parseLong(oldVersion));
    }

    /**
     * 禁用API密钥，带CAS乐观锁
     *
     * @param gatewayId  网关标识
     * @param apiKey     API密钥
     * @param oldVersion 旧版本号
     * @return 禁用结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableApiKey(String gatewayId, String apiKey, String oldVersion) {
        BizAssert.notBlank(gatewayId, "网关ID不能为空");
        BizAssert.notBlank(apiKey, "API Key不能为空");
        BizAssert.notBlank(oldVersion, "版本号不能为空");
        return adminRepository.updateApiKeyStatusByCas(gatewayId, apiKey, DISABLE, Long.parseLong(oldVersion));
    }

    /**
     * 吊销API密钥
     *
     * @param gatewayId 网关标识
     * @param apiKey    API密钥
     * @return 吊销结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean revokeApiKey(String gatewayId, String apiKey) {
        BizAssert.notBlank(gatewayId, "网关ID不能为空");
        return adminRepository.revokeApiKey(gatewayId, apiKey);
    }

    // ==================== 运维 ====================

    /**
     * 发布网关配置
     *
     * @param gatewayId 网关唯一标识
     * @return 发布结果
     */
    @Override
    public boolean publishGateway(String gatewayId) {
        BizAssert.notBlank(gatewayId, "网关ID不能为空");
        return adminRepository.publishGateway(gatewayId);
    }

    /**
     * 下线网关服务
     *
     * @param gatewayId 网关唯一标识
     * @return 下线结果
     */
    @Override
    public boolean offlineGateway(String gatewayId) {
        BizAssert.notBlank(gatewayId, "网关ID不能为空");
        return adminRepository.offlineGateway(gatewayId);
    }

    /**
     * 同步网关配置
     *
     * @param gatewayId 网关唯一标识
     * @return 同步结果
     */
    @Override
    public boolean syncGatewayConfig(String gatewayId) {
        BizAssert.notBlank(gatewayId, "网关ID不能为空");
        return adminRepository.syncGatewayConfig(gatewayId);
    }

    /**
     * 清理网关分布式缓存
     *
     * @param gatewayId 网关唯一标识
     */
    @Override
    public void evictGatewayCache(String gatewayId) {
        BizAssert.notBlank(gatewayId, "网关ID不能为空");
        log.info("清理网关缓存 gatewayId:{}", gatewayId);
    }
}