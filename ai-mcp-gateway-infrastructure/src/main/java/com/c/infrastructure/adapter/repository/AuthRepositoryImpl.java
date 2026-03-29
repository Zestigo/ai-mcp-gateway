package com.c.infrastructure.adapter.repository;

import com.c.domain.auth.adapter.repository.AuthRepository;
import com.c.domain.auth.model.entity.LicenseCommandEntity;
import com.c.domain.auth.model.valobj.McpGatewayAuthVO;
import com.c.domain.auth.model.valobj.McpGatewayCompositeVO;
import com.c.domain.auth.model.valobj.enums.AuthStatusEnum;
import com.c.infrastructure.dao.McpGatewayAuthDao;
import com.c.infrastructure.dao.McpGatewayDao;
import com.c.infrastructure.dao.po.McpGatewayAuthPO;
import com.c.infrastructure.dao.po.McpGatewayCompositePO;
import com.c.infrastructure.dao.po.McpGatewayPO;
import com.c.infrastructure.redis.RedisKeyConstants;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 认证授权仓储层实现
 * 提供API密钥校验、限流控制、授权信息缓存与持久化能力
 * 采用本地缓存+Redis+数据库三级缓存架构
 *
 * @author cyh
 * @date 2026/03/29
 */
@Slf4j
@Repository
public class AuthRepositoryImpl implements AuthRepository {

    /** 网关授权配置数据访问对象 */
    @Resource
    private McpGatewayAuthDao mcpGatewayAuthDao;

    /** 网关基础配置数据访问对象 */
    @Resource
    private McpGatewayDao mcpGatewayDao;

    /** 限流Lua脚本 */
    @Resource
    private DefaultRedisScript<Long> ratelimitLuaScript;

    /** 对象Redis模板 */
    @Resource
    private RedisTemplate<String, McpGatewayCompositeVO> redisTemplate;

    /** 字符串Redis模板 */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 空值缓存对象，用于缓存穿透防护 */
    private static final McpGatewayCompositeVO NULL_VO = new McpGatewayCompositeVO();

    /** 本地缓存：一级缓存，降低Redis与数据库访问压力 */
    private static final Cache<String, McpGatewayCompositeVO> localCache = CacheBuilder
            .newBuilder()
            .concurrencyLevel(Runtime
                    .getRuntime()
                    .availableProcessors() * 2)
            .initialCapacity(1000)
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .maximumSize(10000)
            .build();

    /**
     * 校验网关是否触发限流
     *
     * @param gatewayId     网关唯一标识
     * @param apiKey        API密钥
     * @param limit         限流阈值
     * @param windowSeconds 时间窗口
     * @return true-已限流，false-未限流
     */
    @Override
    public boolean isRateLimited(String gatewayId, String apiKey, int limit, int windowSeconds) {
        String limitKey = RedisKeyConstants.buildRateLimitKey(gatewayId, apiKey);
        try {
            // 执行Lua脚本实现限流
            Long result = stringRedisTemplate.execute(ratelimitLuaScript, Collections.singletonList(limitKey),
                    String.valueOf(limit), String.valueOf(windowSeconds));
            return result == 0;
        } catch (Exception e) {
            log.error("[限流异常] Fail-open兜底放行, Gateway: {}, Reason: {}", gatewayId, e.getMessage());
            // 异常时放行，保证服务可用性
            return false;
        }
    }

    /**
     * 异步查询网关综合授权信息
     * 采用三级缓存：本地缓存->Redis->数据库
     *
     * @param gatewayId 网关唯一标识
     * @param apiKey    API密钥
     * @return 授权信息视图对象
     */
    @Override
    public Mono<McpGatewayCompositeVO> queryCompositeAuth(String gatewayId, String apiKey) {
        String redisKey = RedisKeyConstants.buildAuthKey(gatewayId, apiKey);
        String bizKey = gatewayId + ":" + apiKey;

        // 优先查询本地缓存
        McpGatewayCompositeVO cacheVo = localCache.getIfPresent(bizKey);
        if (cacheVo != null) {
            return cacheVo == NULL_VO ? Mono.empty() : Mono.just(cacheVo);
        }

        // 异步查询Redis和数据库
        return Mono
                .fromCallable(() -> {
                    try {
                        // 查询Redis缓存
                        McpGatewayCompositeVO redisVo = redisTemplate
                                .opsForValue()
                                .get(redisKey);
                        if (redisVo != null) {
                            localCache.put(bizKey, redisVo);
                            return redisVo;
                        }

                        // 查询数据库
                        McpGatewayCompositePO po =
                                mcpGatewayAuthDao.queryCompositeAuth(new McpGatewayCompositePO(gatewayId, apiKey));
                        if (po != null) {
                            // 转换为视图对象
                            McpGatewayCompositeVO vo = convertToVO(po);
                            // 写入两级缓存
                            redisTemplate
                                    .opsForValue()
                                    .set(redisKey, vo, 30, TimeUnit.MINUTES);
                            localCache.put(bizKey, vo);
                            return vo;
                        } else {
                            // 空值缓存，防止穿透
                            redisTemplate
                                    .opsForValue()
                                    .set(redisKey, NULL_VO, 1, TimeUnit.MINUTES);
                            localCache.put(bizKey, NULL_VO);
                            return NULL_VO;
                        }
                    } catch (Exception e) {
                        log.error("[仓储回源异常] Gateway: {}, Reason: {}", gatewayId, e.getMessage());
                        return NULL_VO;
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(vo -> vo == NULL_VO ? Mono.empty() : Mono.just(vo));
    }

    /**
     * 插入网关授权信息
     * 插入后清除缓存保证一致性
     *
     * @param vo 网关授权视图对象
     */
    @Override
    public void insert(McpGatewayAuthVO vo) {
        // 转换为持久化对象
        McpGatewayAuthPO po = McpGatewayAuthPO
                .builder()
                .gatewayId(vo.getGatewayId())
                .apiKey(vo.getApiKey())
                .rateLimit(vo.getRateLimit())
                .expireTime(vo.getExpireTime())
                .status(vo
                        .getStatus()
                        .getCode())
                .build();

        // 执行插入
        mcpGatewayAuthDao.insert(po);

        // 清除缓存，保证数据一致性
        String redisKey = RedisKeyConstants.buildAuthKey(vo.getGatewayId(), vo.getApiKey());
        String bizKey = vo.getGatewayId() + ":" + vo.getApiKey();
        redisTemplate.delete(redisKey);
        localCache.invalidate(bizKey);
        log.info("[仓储持久化] 授权信息更新，已同步失效缓存: {}", bizKey);
    }

    /**
     * 查询网关有效授权数量
     *
     * @param gatewayId 网关唯一标识
     * @return 有效授权数量
     */
    @Override
    public int queryEffectiveGatewayAuthCount(String gatewayId) {
        return mcpGatewayAuthDao.queryEffectiveGatewayAuthCount(gatewayId);
    }

    /**
     * 查询网关有效授权信息
     *
     * @param commandEntity 授权查询命令实体
     * @return 授权信息视图对象
     */
    @Override
    public McpGatewayAuthVO queryEffectiveGatewayAuthInfo(LicenseCommandEntity commandEntity) {
        // 查询授权持久化对象
        McpGatewayAuthPO po = mcpGatewayAuthDao.queryMcpGatewayAuthPO(McpGatewayAuthPO
                .builder()
                .gatewayId(commandEntity.getGatewayId())
                .apiKey(commandEntity.getApiKey())
                .build());
        if (null == po) return null;

        // 转换为视图对象
        return McpGatewayAuthVO
                .builder()
                .gatewayId(po.getGatewayId())
                .apiKey(po.getApiKey())
                .rateLimit(po.getRateLimit())
                .expireTime(po.getExpireTime())
                .status(AuthStatusEnum.AuthConfig.get(po.getStatus()))
                .build();
    }

    /**
     * 查询网关鉴权状态
     *
     * @param gatewayId 网关唯一标识
     * @return 网关鉴权状态枚举
     */
    @Override
    public AuthStatusEnum.GatewayConfig queryGatewayAuthStatus(String gatewayId) {
        McpGatewayPO po = mcpGatewayDao.queryByGatewayId(gatewayId);
        return (po == null) ? AuthStatusEnum.GatewayConfig.STRONG_VERIFIED :
                AuthStatusEnum.GatewayConfig.get(po.getAuth());
    }

    /**
     * 持久化对象转换为视图对象
     *
     * @param po 综合授权持久化对象
     * @return 综合授权视图对象
     */
    private McpGatewayCompositeVO convertToVO(McpGatewayCompositePO po) {
        return McpGatewayCompositeVO
                .builder()
                .gatewayId(po.getGatewayId())
                .auth(po.getAuth())
                .gatewayStatus(po.getGatewayStatus())
                .apiKey(po.getApiKey())
                .authStatus(po.getAuthStatus())
                .rateLimit(po.getRateLimit())
                .expireTime(po.getExpireTime())
                .build();
    }
}