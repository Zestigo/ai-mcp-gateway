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
 * 认证授权仓储层实现类
 * 采用本地缓存+Redis+MySQL三级缓存架构，实现高性能、高并发、防穿透的认证数据查询与持久化
 * 包含限流、缓存维护、数据转换、异常防护等基础设施能力
 *
 * @author cyh
 * @date 2026/03/27
 */
@Slf4j
@Repository
public class AuthRepositoryImpl implements AuthRepository {

    /** 网关授权信息数据库访问对象 */
    @Resource
    private McpGatewayAuthDao mcpGatewayAuthDao;

    /** 网关基础配置信息数据库访问对象 */
    @Resource
    private McpGatewayDao mcpGatewayDao;

    /** 分布式限流原子操作Lua脚本 */
    @Resource
    private DefaultRedisScript<Long> ratelimitLuaScript;

    /** 聚合认证对象Redis序列化操作模板 */
    @Resource
    private RedisTemplate<String, McpGatewayCompositeVO> redisTemplate;

    /** 字符串类型Redis操作模板，用于限流计数 */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 缓存穿透专用空对象，避免重复创建降低GC压力 */
    private static final McpGatewayCompositeVO NULL_VO = new McpGatewayCompositeVO();

    /** L1本地缓存：5秒过期、高并发等级，承载瞬时流量减轻Redis压力 */
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
     * 执行API密钥分布式限流校验
     * 通过Lua脚本保证原子性，异常时采用fail-open策略兜底放行不阻断业务
     *
     * @param gatewayId     网关唯一标识
     * @param apiKey        API访问密钥
     * @param limit         单位时间窗口内最大允许访问次数
     * @param windowSeconds 限流时间窗口大小（秒）
     * @return true=触发限流拦截，false=正常放行
     */
    @Override
    public boolean isRateLimited(String gatewayId, String apiKey, int limit, int windowSeconds) {
        // 构建统一规范的限流Redis键
        String limitKey = RedisKeyConstants.buildRateLimitKey(gatewayId, apiKey);
        try {
            // 执行Lua脚本实现原子限流计数，返回0表示超限，1表示正常
            Long result = stringRedisTemplate.execute(ratelimitLuaScript, Collections.singletonList(limitKey),
                    String.valueOf(limit), String.valueOf(windowSeconds));
            return result == 0;
        } catch (Exception e) {
            // 限流服务异常时记录日志并兜底放行，避免基础组件异常影响核心业务
            log.error("[分布式限流异常] 触发Fail-open兜底放行, Gateway: {}, Reason: {}", gatewayId, e.getMessage());
            return false;
        }
    }

    /**
     * 三级缓存架构聚合查询网关全量认证信息
     * 优先本地缓存→其次Redis缓存→最终数据库回源，自带缓存穿透防护
     *
     * @param gatewayId 网关唯一标识
     * @param apiKey    API访问密钥
     * @return 响应式返回聚合认证信息对象
     */
    @Override
    public Mono<McpGatewayCompositeVO> queryCompositeAuth(String gatewayId, String apiKey) {
        // 构建Redis缓存键与本地缓存业务键
        String redisKey = RedisKeyConstants.buildAuthKey(gatewayId, apiKey);
        String bizKey = gatewayId + ":" + apiKey;

        // 第一步：查询L1本地缓存，命中则直接返回结果
        McpGatewayCompositeVO cacheVo = localCache.getIfPresent(bizKey);
        if (cacheVo != null) {
            // 空对象代表缓存穿透标记，返回空Mono；正常对象直接返回
            return cacheVo == NULL_VO ? Mono.empty() : Mono.just(cacheVo);
        }

        // 本地缓存未命中，异步调度线程池查询Redis与数据库
        return Mono
                .fromCallable(() -> {
                    try {
                        // 第二步：查询L2 Redis分布式缓存
                        McpGatewayCompositeVO redisVo = redisTemplate
                                .opsForValue()
                                .get(redisKey);
                        if (redisVo != null) {
                            // Redis命中，回写本地缓存提升后续查询性能
                            localCache.put(bizKey, redisVo);
                            return redisVo;
                        }

                        // 第三步：Redis未命中，回源L3 MySQL数据库查询
                        McpGatewayCompositePO po =
                                mcpGatewayAuthDao.queryCompositeAuth(new McpGatewayCompositePO(gatewayId, apiKey));
                        if (po != null) {
                            // 数据库数据转换为领域值对象
                            McpGatewayCompositeVO vo = convertToVO(po);
                            // 写入Redis缓存（30分钟过期）与本地缓存
                            redisTemplate
                                    .opsForValue()
                                    .set(redisKey, vo, 30, TimeUnit.MINUTES);
                            localCache.put(bizKey, vo);
                            return vo;
                        } else {
                            // 数据库无数据，执行缓存穿透防护：写入空对象标记（1分钟过期）
                            redisTemplate
                                    .opsForValue()
                                    .set(redisKey, NULL_VO, 1, TimeUnit.MINUTES);
                            localCache.put(bizKey, NULL_VO);
                            return NULL_VO;
                        }
                    } catch (Exception e) {
                        // 缓存/数据库异常时记录日志，返回空对象防止数据库击穿
                        log.error("[缓存回源异常] Gateway: {}, Reason: {}", gatewayId, e.getMessage());
                        return NULL_VO;
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(vo -> vo == NULL_VO ? Mono.empty() : Mono.just(vo));
    }

    /**
     * 持久化网关授权信息
     * 数据插入后主动清理两级缓存，保证数据一致性
     *
     * @param vo 网关授权领域值对象
     */
    @Override
    public void insert(McpGatewayAuthVO vo) {
        // 值对象转换为数据库持久化对象
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

        // 执行数据库插入操作
        mcpGatewayAuthDao.insert(po);

        // Cache Aside模式：数据更新后主动清理Redis与本地缓存
        String redisKey = RedisKeyConstants.buildAuthKey(vo.getGatewayId(), vo.getApiKey());
        redisTemplate.delete(redisKey);
        localCache.invalidate(vo.getGatewayId() + ":" + vo.getApiKey());
        log.info("[仓储持久化] 网关授权已更新并清理缓存: {}", vo.getGatewayId());
    }

    /**
     * 查询网关下有效授权记录数量
     * 用于注册防重校验
     *
     * @param gatewayId 网关唯一标识
     * @return 有效授权记录条数
     */
    @Override
    public int queryEffectiveGatewayAuthCount(String gatewayId) {
        return mcpGatewayAuthDao.queryEffectiveGatewayAuthCount(gatewayId);
    }

    /**
     * 查询网关有效授权配置信息
     *
     * @param commandEntity 证书校验命令实体
     * @return 网关授权值对象，无数据返回null
     */
    @Override
    public McpGatewayAuthVO queryEffectiveGatewayAuthInfo(LicenseCommandEntity commandEntity) {
        // 构建查询参数对象
        McpGatewayAuthPO po = mcpGatewayAuthDao.queryMcpGatewayAuthPO(McpGatewayAuthPO
                .builder()
                .gatewayId(commandEntity.getGatewayId())
                .apiKey(commandEntity.getApiKey())
                .build());
        if (null == po) return null;

        // 数据库对象转换为领域值对象返回
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
     * 查询网关全局鉴权开关配置
     *
     * @param gatewayId 网关唯一标识
     * @return 网关鉴权配置枚举
     */
    @Override
    public AuthStatusEnum.GatewayConfig queryGatewayAuthStatus(String gatewayId) {
        McpGatewayPO po = mcpGatewayDao.queryMcpGatewayByGatewayId(gatewayId);
        // 无网关配置时默认返回强校验模式
        return (po == null) ? AuthStatusEnum.GatewayConfig.STRONG_VERIFIED :
                AuthStatusEnum.GatewayConfig.get(po.getAuth());
    }

    /**
     * 数据库持久化对象转换为领域聚合值对象
     *
     * @param po 数据库查询结果对象
     * @return 领域层聚合认证值对象
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