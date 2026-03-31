package com.c.infrastructure.adapter.repository;

import com.c.domain.admin.adapter.repository.AdminRepository;
import com.c.domain.admin.model.entity.*;
import com.c.domain.admin.model.valobj.PageResponse;
import com.c.infrastructure.dao.*;
import com.c.infrastructure.dao.po.McpGatewayAuthPO;
import com.c.infrastructure.dao.po.McpGatewayPO;
import com.c.infrastructure.dao.po.McpGatewayToolPO;
import com.c.infrastructure.dao.po.McpProtocolHttpPO;
import com.c.infrastructure.redis.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 管理员仓库实现类
 * 负责网关配置、工具配置、协议配置和认证信息的持久化操作
 * 包含缓存管理、数据转换和事务控制等功能
 *
 * @author cyh
 * @date 2026/03/31
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AdminRepositoryImpl implements AdminRepository {

    /** 网关基础配置DAO */
    private final McpGatewayDao mcpGatewayDao;
    /** 网关认证配置DAO */
    private final McpGatewayAuthDao mcpGatewayAuthDao;
    /** 网关工具配置DAO */
    private final McpGatewayToolDao mcpGatewayToolDao;
    /** HTTP协议配置DAO */
    private final McpProtocolHttpDao mcpProtocolHttpDao;
    /** Redis缓存操作模板 */
    private final RedisTemplate<String, Object> redisTemplate;

    /** 空值缓存过期时间，单位：秒，用于防止缓存穿透 */
    private static final long CACHE_NULL_TTL = 60L;

    // ===================== 1. 网关管理 =====================

    /**
     * 分页查询网关配置信息
     *
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @param keyword  搜索关键词
     * @param status   网关状态
     * @return 分页结果对象，包含网关配置实体列表和分页信息
     */
    @Override
    public PageResponse<GatewayConfigEntity> queryGatewayConfigPage(int pageNo, int pageSize, String keyword,
                                                                    Integer status) {
        // 校验并修正分页参数，确保页码和页长最小为1
        int validPageNo = Math.max(1, pageNo);
        int validPageSize = Math.max(1, pageSize);
        // 计算数据库查询偏移量
        int offset = (validPageNo - 1) * validPageSize;

        // 分页查询数据库获取网关配置持久化对象
        List<McpGatewayPO> poList = mcpGatewayDao.queryGatewayConfigPage(offset, validPageSize, keyword, status);
        // 查询总记录数
        long total = mcpGatewayDao.queryGatewayConfigCount(keyword, status);

        // PO对象转换为领域实体
        List<GatewayConfigEntity> entityList = poList
                .stream()
                .map(this::convertToGatewayEntity)
                .collect(Collectors.toList());

        // 封装并返回分页响应结果
        return PageResponse.of(entityList, validPageNo, validPageSize, total);
    }

    /**
     * 根据网关ID查询网关配置信息（带Redis缓存）
     *
     * @param gatewayId 网关唯一标识
     * @return 网关配置领域实体，不存在则返回null
     * @throws IllegalArgumentException 当网关ID为空时抛出
     */
    @Override
    public GatewayConfigEntity findGatewayById(String gatewayId) {
        // 参数非空校验
        Assert.hasText(gatewayId, "网关ID不能为空");
        // 构建Redis缓存Key
        RedisKey.KeyDefinition keyDef = RedisKey.GW_CONFIG.build(gatewayId);

        // 优先从Redis缓存中查询数据
        GatewayConfigEntity cache = (GatewayConfigEntity) redisTemplate
                .opsForValue()
                .get(keyDef.getKey());
        if (cache != null) {
            return cache;
        }

        // 缓存未命中，查询数据库
        McpGatewayPO po = mcpGatewayDao.findGatewayById(gatewayId);
        // 数据库也不存在，缓存空值防止穿透
        if (po == null) {
            redisTemplate
                    .opsForValue()
                    .set(keyDef.getKey(), "", CACHE_NULL_TTL, TimeUnit.SECONDS);
            return null;
        }

        // PO转换为领域实体
        GatewayConfigEntity entity = convertToGatewayEntity(po);
        // 将查询结果写入Redis缓存
        redisTemplate
                .opsForValue()
                .set(keyDef.getKey(), entity, keyDef
                        .getTtl()
                        .getSeconds(), TimeUnit.SECONDS);
        return entity;
    }

    /**
     * 创建网关配置（事务控制）
     *
     * @param entity 网关配置领域实体
     * @return 创建成功返回true，失败返回false
     * @throws IllegalArgumentException 当实体或网关ID为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public boolean createGateway(GatewayConfigEntity entity) {
        // 参数合法性校验
        Assert.notNull(entity, "网关实体不能为空");
        Assert.hasText(entity.getGatewayId(), "网关ID不能为空");

        // 领域实体转换为PO对象
        McpGatewayPO po = convertToGatewayPO(entity);
        // 执行数据库插入操作
        boolean success = mcpGatewayDao.insertGateway(po) > 0;

        // 插入成功，清理相关缓存保证数据一致性
        if (success) {
            evictGatewayAllCache(entity.getGatewayId());
        }
        return success;
    }

    /**
     * 基于乐观锁更新网关配置（事务控制）
     *
     * @param entity     网关配置领域实体
     * @param oldVersion 乐观锁版本号
     * @return 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当实体、网关ID或版本号为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public boolean updateGatewayByCas(GatewayConfigEntity entity, Long oldVersion) {
        // 参数合法性校验
        Assert.notNull(entity, "网关实体不能为空");
        Assert.hasText(entity.getGatewayId(), "网关ID不能为空");
        Assert.notNull(oldVersion, "乐观锁版本号不能为空");

        // 领域实体转换为PO对象
        McpGatewayPO po = convertToGatewayPO(entity);
        // 执行CAS更新
        boolean success = mcpGatewayDao.updateGatewayByCas(po, oldVersion) > 0;

        // 更新成功，清理缓存
        if (success) {
            evictGatewayAllCache(entity.getGatewayId());
        }
        return success;
    }

    /**
     * 基于乐观锁更新网关状态（事务控制）
     *
     * @param gatewayId  网关唯一标识
     * @param oldVersion 乐观锁版本号
     * @param status     目标状态
     * @return 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public boolean updateGatewayStatusByCas(String gatewayId, Long oldVersion, Integer status) {
        // 参数非空校验
        Assert.hasText(gatewayId, "网关ID不能为空");
        Assert.notNull(oldVersion, "乐观锁版本号不能为空");

        // 执行状态更新
        boolean success = mcpGatewayDao.updateGatewayStatusByCas(gatewayId, oldVersion, status) > 0;
        // 更新成功清理缓存
        if (success) {
            evictGatewayAllCache(gatewayId);
        }
        return success;
    }

    /**
     * 删除网关配置（事务控制）
     *
     * @param gatewayId 网关唯一标识
     * @return 删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当网关ID为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public boolean deleteGateway(String gatewayId) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        boolean success = mcpGatewayDao.deleteGateway(gatewayId) > 0;
        if (success) {
            evictGatewayAllCache(gatewayId);
        }
        return success;
    }

    /**
     * 发布网关（事务控制）
     *
     * @param gatewayId 网关唯一标识
     * @return 发布成功返回true，失败返回false
     * @throws IllegalArgumentException 当网关ID为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public boolean publishGateway(String gatewayId) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        boolean success = mcpGatewayDao.publishGateway(gatewayId) > 0;
        if (success) {
            evictGatewayAllCache(gatewayId);
        }
        return success;
    }

    /**
     * 下线网关（事务控制）
     *
     * @param gatewayId 网关唯一标识
     * @return 下线成功返回true，失败返回false
     * @throws IllegalArgumentException 当网关ID为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public boolean offlineGateway(String gatewayId) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        boolean success = mcpGatewayDao.offlineGateway(gatewayId) > 0;
        if (success) {
            evictGatewayAllCache(gatewayId);
        }
        return success;
    }

    /**
     * 同步网关配置，清理缓存实现配置刷新
     *
     * @param gatewayId 网关唯一标识
     * @return 同步成功固定返回true
     * @throws IllegalArgumentException 当网关ID为空时抛出
     */
    @Override
    public boolean syncGatewayConfig(String gatewayId) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        evictGatewayAllCache(gatewayId);
        return true;
    }

    // ===================== 2. 网关工具管理 =====================

    /**
     * 分页查询网关工具配置
     *
     * @param pageNo     页码
     * @param pageSize   每页条数
     * @param gatewayId  网关ID
     * @param protocolId 协议ID
     * @param toolStatus 工具状态
     * @return 分页结果对象，包含工具配置实体列表
     */
    @Override
    public PageResponse<GatewayToolConfigEntity> queryToolPage(int pageNo, int pageSize, String gatewayId,
                                                               Long protocolId, Integer toolStatus) {
        // 校验分页参数
        int validPageNo = Math.max(1, pageNo);
        int validPageSize = Math.max(1, pageSize);
        int offset = (validPageNo - 1) * validPageSize;

        // 分页查询数据库
        List<McpGatewayToolPO> poList = mcpGatewayToolDao.queryGatewayToolConfigPage(offset, validPageSize, gatewayId
                , protocolId, toolStatus);
        long total = mcpGatewayToolDao.queryGatewayToolConfigCount(gatewayId, protocolId, toolStatus);

        // PO转实体
        List<GatewayToolConfigEntity> entityList = poList
                .stream()
                .map(this::convertToToolEntity)
                .collect(Collectors.toList());

        return PageResponse.of(entityList, validPageNo, validPageSize, total);
    }

    /**
     * 根据网关ID查询绑定的所有工具配置
     *
     * @param gatewayId 网关唯一标识
     * @return 工具配置实体列表，无数据返回空集合
     * @throws IllegalArgumentException 当网关ID为空时抛出
     */
    @Override
    public List<GatewayToolConfigEntity> listToolsByGateway(String gatewayId) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        List<McpGatewayToolPO> poList = mcpGatewayToolDao.getToolsByGatewayId(gatewayId);
        if (CollectionUtils.isEmpty(poList)) {
            return Collections.emptyList();
        }
        return poList
                .stream()
                .map(this::convertToToolEntity)
                .collect(Collectors.toList());
    }

    /**
     * 绑定网关工具（事务控制）
     *
     * @param entity 工具配置领域实体
     * @return 绑定成功返回true，失败返回false
     * @throws IllegalArgumentException 当实体为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public boolean bindTool(GatewayToolConfigEntity entity) {
        Assert.notNull(entity, "工具配置不能为空");
        McpGatewayToolPO po = convertToToolPO(entity);
        boolean success = mcpGatewayToolDao.addToolConfig(po) > 0;
        if (success) {
            clearGatewayToolCache(entity.getGatewayId());
        }
        return success;
    }

    /**
     * 解绑网关工具（事务控制）
     *
     * @param gatewayId  网关唯一标识
     * @param toolId     工具ID
     * @param protocolId 协议ID
     * @return 解绑成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public boolean unbindTool(String gatewayId, Integer toolId, Long protocolId) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        Assert.notNull(toolId, "工具ID不能为空");
        Assert.notNull(protocolId, "协议ID不能为空");

        boolean success = mcpGatewayToolDao.removeToolFromGateway(gatewayId, protocolId, Long.valueOf(toolId)) > 0;
        if (success) {
            clearGatewayToolCache(gatewayId);
        }
        return success;
    }

    /**
     * 判断工具是否已绑定到网关
     *
     * @param gatewayId  网关唯一标识
     * @param toolId     工具ID
     * @param protocolId 协议ID
     * @return 已绑定返回true，未绑定返回false
     * @throws IllegalArgumentException 当参数为空时抛出
     */
    @Override
    public boolean isToolExist(String gatewayId, Integer toolId, Long protocolId) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        Assert.notNull(toolId, "工具ID不能为空");
        Assert.notNull(protocolId, "协议ID不能为空");
        return mcpGatewayToolDao.isToolConfigured(gatewayId, protocolId, Long.valueOf(toolId)) > 0;
    }

    /**
     * 基于乐观锁更新工具状态（事务控制）
     *
     * @param gatewayId  网关唯一标识
     * @param toolId     工具ID
     * @param protocolId 协议ID
     * @param status     目标状态
     * @param oldVersion 乐观锁版本号
     * @return 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public boolean updateToolStatusByCas(String gatewayId, Integer toolId, Long protocolId, Integer status,
                                         Long oldVersion) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        Assert.notNull(toolId, "工具ID不能为空");
        Assert.notNull(protocolId, "协议ID不能为空");
        Assert.notNull(oldVersion, "乐观锁版本号不能为空");

        boolean success = mcpGatewayToolDao.updateToolStatusByCas(gatewayId, protocolId, Long.valueOf(toolId), status
                , oldVersion) > 0;
        if (success) {
            clearGatewayToolCache(gatewayId);
        }
        return success;
    }

    /**
     * 基于乐观锁更新工具完整配置（事务控制）
     *
     * @param entity     工具配置领域实体
     * @param oldVersion 乐观锁版本号
     * @return 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当实体或版本号为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public boolean updateToolConfigByCas(GatewayToolConfigEntity entity, Long oldVersion) {
        Assert.notNull(entity, "工具配置不能为空");
        Assert.notNull(oldVersion, "乐观锁版本号不能为空");

        McpGatewayToolPO po = convertToToolPO(entity);
        boolean success = mcpGatewayToolDao.updateToolConfigByCas(po, oldVersion) > 0;
        if (success) {
            clearGatewayToolCache(entity.getGatewayId());
        }
        return success;
    }

    // ===================== 3. 协议管理 =====================

    /**
     * 分页查询HTTP协议配置
     *
     * @param pageNo     页码
     * @param pageSize   每页条数
     * @param protocolId 协议ID
     * @param status     协议状态
     * @return 分页结果对象，包含协议配置实体列表
     */
    @Override
    public PageResponse<GatewayProtocolEntity> queryProtocolConfigPage(int pageNo, int pageSize, Long protocolId,
                                                                       Integer status) {
        int validPageNo = Math.max(1, pageNo);
        int validPageSize = Math.max(1, pageSize);
        int offset = (validPageNo - 1) * validPageSize;

        List<McpProtocolHttpPO> poList = mcpProtocolHttpDao.queryProtocolConfigPage(offset, validPageSize, protocolId
                , status);
        long total = mcpProtocolHttpDao.queryProtocolConfigCount(protocolId, status);

        List<GatewayProtocolEntity> entityList = poList
                .stream()
                .map(this::convertToProtocolEntity)
                .collect(Collectors.toList());

        return PageResponse.of(entityList, validPageNo, validPageSize, total);
    }

    /**
     * 查询协议配置列表
     *
     * @param protocolId 协议ID
     * @param status     协议状态
     * @return 协议配置实体列表，无数据返回空集合
     */
    @Override
    public List<GatewayProtocolEntity> getProtocolConfigList(Long protocolId, Integer status) {
        List<McpProtocolHttpPO> poList = mcpProtocolHttpDao.getProtocolConfigList(protocolId, status);
        if (CollectionUtils.isEmpty(poList)) {
            return Collections.emptyList();
        }
        return poList
                .stream()
                .map(this::convertToProtocolEntity)
                .collect(Collectors.toList());
    }

    /**
     * 根据协议ID查询协议配置
     *
     * @param protocolId 协议唯一标识
     * @return 协议配置领域实体
     * @throws IllegalArgumentException 当协议ID为空时抛出
     */
    @Override
    public GatewayProtocolEntity findByProtocolId(Long protocolId) {
        Assert.notNull(protocolId, "协议ID不能为空");
        McpProtocolHttpPO po = mcpProtocolHttpDao.findProtocolById(protocolId);
        return convertToProtocolEntity(po);
    }

    /**
     * 创建HTTP协议配置（事务控制）
     *
     * @param entity 协议配置领域实体
     * @return 创建成功返回true，失败返回false
     * @throws IllegalArgumentException 当实体为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public boolean createProtocol(GatewayProtocolEntity entity) {
        Assert.notNull(entity, "协议实体不能为空");
        McpProtocolHttpPO po = convertToProtocolPO(entity);
        return mcpProtocolHttpDao.createProtocol(po) > 0;
    }

    /**
     * 基于乐观锁更新协议配置（事务控制）
     *
     * @param entity     协议配置领域实体
     * @param oldVersion 乐观锁版本号
     * @return 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当实体或版本号为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public boolean updateProtocolConfigByCas(GatewayProtocolEntity entity, Long oldVersion) {
        Assert.notNull(entity, "协议实体不能为空");
        Assert.notNull(oldVersion, "乐观锁版本号不能为空");

        McpProtocolHttpPO po = convertToProtocolPO(entity);
        return mcpProtocolHttpDao.updateProtocolConfigByCas(po, oldVersion) > 0;
    }

    /**
     * 根据ID删除协议配置（事务控制）
     *
     * @param protocolId 协议唯一标识
     * @return 删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当协议ID为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public boolean deleteProtocolById(Long protocolId) {
        Assert.notNull(protocolId, "协议ID不能为空");
        return mcpProtocolHttpDao.deleteByProtocolId(protocolId) > 0;
    }

    /**
     * 基于乐观锁更新协议状态（事务控制）
     *
     * @param protocolId 协议唯一标识
     * @param status     目标状态
     * @param oldVersion 乐观锁版本号
     * @return 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public boolean updateProtocolStatusByCas(Long protocolId, Integer status, Long oldVersion) {
        Assert.notNull(protocolId, "协议ID不能为空");
        Assert.notNull(status, "状态不能为空");
        Assert.notNull(oldVersion, "乐观锁版本号不能为空");

        return mcpProtocolHttpDao.updateProtocolStatusByCas(protocolId, status, oldVersion) > 0;
    }

    // ===================== 4. 鉴权管理 =====================

    /**
     * 分页查询网关认证配置
     *
     * @param pageNo    页码
     * @param pageSize  每页条数
     * @param gatewayId 网关ID
     * @return 分页结果对象，包含认证配置实体列表
     */
    @Override
    public PageResponse<GatewayAuthEntity> queryGatewayAuthPage(int pageNo, int pageSize, String gatewayId) {
        int validPageNo = Math.max(1, pageNo);
        int validPageSize = Math.max(1, pageSize);
        int offset = (validPageNo - 1) * validPageSize;

        List<McpGatewayAuthPO> poList = mcpGatewayAuthDao.queryGatewayAuthPage(offset, validPageSize, gatewayId);
        long total = mcpGatewayAuthDao.queryGatewayAuthCount(gatewayId);

        List<GatewayAuthEntity> entityList = poList
                .stream()
                .map(this::convertToAuthEntity)
                .collect(Collectors.toList());

        return PageResponse.of(entityList, validPageNo, validPageSize, total);
    }

    /**
     * 根据网关ID查询所有认证配置
     *
     * @param gatewayId 网关唯一标识
     * @return 认证配置实体列表，无数据返回空集合
     * @throws IllegalArgumentException 当网关ID为空时抛出
     */
    @Override
    public List<GatewayAuthEntity> getAuthListByGatewayId(String gatewayId) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        List<McpGatewayAuthPO> poList = mcpGatewayAuthDao.getAuthListByGatewayId(gatewayId);
        if (CollectionUtils.isEmpty(poList)) {
            return Collections.emptyList();
        }
        return poList
                .stream()
                .map(this::convertToAuthEntity)
                .collect(Collectors.toList());
    }

    /**
     * 根据ApiKey查询认证信息（带Redis缓存）
     *
     * @param apiKey 认证密钥
     * @return 认证配置领域实体，不存在返回null
     * @throws IllegalArgumentException 当ApiKey为空时抛出
     */
    @Override
    public GatewayAuthEntity findAuthByApiKey(String apiKey) {
        Assert.hasText(apiKey, "apiKey不能为空");
        RedisKey.KeyDefinition keyDef = RedisKey.GW_API_KEY.build(apiKey);

        // 从Redis缓存查询
        GatewayAuthEntity cache = (GatewayAuthEntity) redisTemplate
                .opsForValue()
                .get(keyDef.getKey());
        if (cache != null) {
            return cache;
        }

        // 缓存未命中，查询数据库
        McpGatewayAuthPO po = mcpGatewayAuthDao.findAuthByApiKey(apiKey);
        if (po == null) {
            return null;
        }

        // 转换并写入缓存
        GatewayAuthEntity entity = convertToAuthEntity(po);
        redisTemplate
                .opsForValue()
                .set(keyDef.getKey(), entity, keyDef
                        .getTtl()
                        .getSeconds(), TimeUnit.SECONDS);
        return entity;
    }

    /**
     * 判断ApiKey是否已存在
     *
     * @param apiKey 认证密钥
     * @return 存在返回true，不存在返回false
     * @throws IllegalArgumentException 当ApiKey为空时抛出
     */
    @Override
    public boolean isApiKeyExists(String apiKey) {
        Assert.hasText(apiKey, "apiKey不能为空");
        return mcpGatewayAuthDao.isApiKeyExists(apiKey) > 0;
    }

    /**
     * 保存网关认证配置（事务控制+缓存清理）
     *
     * @param entity 认证配置领域实体
     * @return 保存成功返回true，失败返回false
     * @throws IllegalArgumentException 当实体为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public boolean saveAuth(GatewayAuthEntity entity) {
        Assert.notNull(entity, "鉴权实体不能为空");
        McpGatewayAuthPO po = convertToAuthPO(entity);
        boolean success = mcpGatewayAuthDao.saveAuth(po) > 0;

        // 保存成功，清理相关缓存
        if (success) {
            redisTemplate.delete(RedisKey.GW_AUTH_LIST.getKey(entity.getGatewayId()));
            redisTemplate.delete(RedisKey.GW_API_KEY.getKey(entity.getApiKey()));
        }
        return success;
    }

    /**
     * 基于乐观锁更新ApiKey状态（事务控制）
     *
     * @param gatewayId  网关唯一标识
     * @param apiKey     认证密钥
     * @param status     目标状态
     * @param oldVersion 乐观锁版本号
     * @return 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public boolean updateApiKeyStatusByCas(String gatewayId, String apiKey, Integer status, Long oldVersion) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        Assert.hasText(apiKey, "apiKey不能为空");
        Assert.notNull(oldVersion, "乐观锁版本号不能为空");

        boolean success = mcpGatewayAuthDao.updateApiKeyStatusByCas(gatewayId, apiKey, status, oldVersion) > 0;
        if (success) {
            redisTemplate.delete(RedisKey.GW_AUTH_LIST.getKey(gatewayId));
            redisTemplate.delete(RedisKey.GW_API_KEY.getKey(apiKey));
        }
        return success;
    }

    /**
     * 吊销ApiKey（事务控制）
     *
     * @param gatewayId 网关唯一标识
     * @param apiKey    认证密钥
     * @return 吊销成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public boolean revokeApiKey(String gatewayId, String apiKey) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        Assert.hasText(apiKey, "apiKey不能为空");

        boolean success = mcpGatewayAuthDao.revokeApiKey(gatewayId, apiKey) > 0;
        if (success) {
            redisTemplate.delete(RedisKey.GW_AUTH_LIST.getKey(gatewayId));
            redisTemplate.delete(RedisKey.GW_API_KEY.getKey(apiKey));
        }
        return success;
    }

    // ===================== 缓存工具 =====================

    /**
     * 清理网关所有相关缓存（配置+工具+认证）
     *
     * @param gatewayId 网关唯一标识
     */
    private void evictGatewayAllCache(String gatewayId) {
        if (gatewayId == null) return;
        // 清理网关基础配置缓存
        redisTemplate.delete(RedisKey.GW_CONFIG.getKey(gatewayId));
        // 清理网关工具缓存
        clearGatewayToolCache(gatewayId);
        // 清理网关认证列表缓存
        redisTemplate.delete(RedisKey.GW_AUTH_LIST.getKey(gatewayId));
    }

    /**
     * 清理网关工具相关缓存
     *
     * @param gatewayId 网关唯一标识
     */
    private void clearGatewayToolCache(String gatewayId) {
        if (gatewayId == null) return;
        // 通配符删除所有网关工具缓存
        redisTemplate.delete(RedisKey.GW_TOOL.getKey(gatewayId) + "*");
    }

    /**
     * 网关PO转领域实体
     *
     * @param po 网关持久化对象
     * @return 网关配置领域实体
     */
    private GatewayConfigEntity convertToGatewayEntity(McpGatewayPO po) {
        if (po == null) return null;
        GatewayConfigEntity entity = new GatewayConfigEntity();
        BeanUtils.copyProperties(po, entity);
        return entity;
    }

    /**
     * 网关领域实体转PO
     *
     * @param e 网关配置领域实体
     * @return 网关持久化对象
     */
    private McpGatewayPO convertToGatewayPO(GatewayConfigEntity e) {
        if (e == null) return null;
        McpGatewayPO po = new McpGatewayPO();
        BeanUtils.copyProperties(e, po);
        return po;
    }

    /**
     * 工具PO转领域实体
     *
     * @param po 工具持久化对象
     * @return 工具配置领域实体
     */
    private GatewayToolConfigEntity convertToToolEntity(McpGatewayToolPO po) {
        if (po == null) return null;
        GatewayToolConfigEntity entity = new GatewayToolConfigEntity();
        BeanUtils.copyProperties(po, entity);
        return entity;
    }

    /**
     * 工具领域实体转PO
     *
     * @param e 工具配置领域实体
     * @return 工具持久化对象
     */
    private McpGatewayToolPO convertToToolPO(GatewayToolConfigEntity e) {
        if (e == null) return null;
        McpGatewayToolPO po = new McpGatewayToolPO();
        BeanUtils.copyProperties(e, po);
        return po;
    }

    /**
     * 协议PO转领域实体
     *
     * @param po 协议持久化对象
     * @return 协议配置领域实体
     */
    private GatewayProtocolEntity convertToProtocolEntity(McpProtocolHttpPO po) {
        if (po == null) return null;
        GatewayProtocolEntity entity = new GatewayProtocolEntity();
        BeanUtils.copyProperties(po, entity);
        return entity;
    }

    /**
     * 协议领域实体转PO
     *
     * @param e 协议配置领域实体
     * @return 协议持久化对象
     */
    private McpProtocolHttpPO convertToProtocolPO(GatewayProtocolEntity e) {
        if (e == null) return null;
        McpProtocolHttpPO po = new McpProtocolHttpPO();
        BeanUtils.copyProperties(e, po);
        return po;
    }

    /**
     * 认证PO转领域实体
     *
     * @param po 认证持久化对象
     * @return 认证配置领域实体
     */
    private GatewayAuthEntity convertToAuthEntity(McpGatewayAuthPO po) {
        if (po == null) return null;
        GatewayAuthEntity entity = new GatewayAuthEntity();
        BeanUtils.copyProperties(po, entity);
        return entity;
    }

    /**
     * 认证领域实体转PO
     *
     * @param e 认证配置领域实体
     * @return 认证持久化对象
     */
    private McpGatewayAuthPO convertToAuthPO(GatewayAuthEntity e) {
        if (e == null) return null;
        McpGatewayAuthPO po = new McpGatewayAuthPO();
        BeanUtils.copyProperties(e, po);
        return po;
    }
}