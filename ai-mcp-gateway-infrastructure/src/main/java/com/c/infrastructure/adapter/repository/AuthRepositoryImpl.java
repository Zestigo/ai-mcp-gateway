package com.c.infrastructure.adapter.repository;

import cn.hutool.core.util.StrUtil;
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
import com.c.infrastructure.redis.RedisKey;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
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
 * @date 2026/03/31
 */
@Slf4j
@Repository
public class AuthRepositoryImpl implements AuthRepository {

    // ===================== 依赖注入 =====================
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

    // ===================== 全局常量 =====================
    /** 空值缓存对象，用于缓存穿透防护 */
    private static final McpGatewayCompositeVO NULL_VO = new McpGatewayCompositeVO();
    /** 空值缓存过期时间：1分钟 */
    private static final long NULL_CACHE_SECONDS = 60L;

    // ===================== 一级本地缓存（性能优化） =====================
    /**
     * 本地缓存：基于Guava实现，提升高频访问性能
     * 并发级别：CPU核心数*2
     * 初始容量：1000
     * 写入过期：5秒
     * 最大容量：10000
     */
    private static final Cache<String, McpGatewayCompositeVO> LOCAL_CACHE = CacheBuilder
            .newBuilder()
            .concurrencyLevel(Runtime
                    .getRuntime()
                    .availableProcessors() * 2)
            .initialCapacity(1000)
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .maximumSize(10000)
            .build();

    // ===================== 核心接口实现 =====================

    /**
     * 校验网关是否触发限流
     *
     * @param gatewayId     网关唯一标识
     * @param apiKey        认证密钥
     * @param limit         限流阈值
     * @param windowSeconds 限流时间窗口
     * @return 触发限流返回true，正常返回false；参数非法/Redis异常返回false（熔断保护）
     */
    @Override
    public boolean isRateLimited(String gatewayId, String apiKey, int limit, int windowSeconds) {
        // 参数校验
        if (StrUtil.isBlank(gatewayId) || StrUtil.isBlank(apiKey) || limit <= 0 || windowSeconds <= 0) {
            log.warn("[限流校验] 参数非法，gatewayId:{}, apiKey:{}", gatewayId, apiKey);
            return false;
        }

        // 统一RedisKey管理
        String redisKey = RedisKey.RATE_LIMIT.getKey(gatewayId, apiKey);

        try {
            // 执行Lua限流脚本，保证原子性
            Long result = stringRedisTemplate.execute(ratelimitLuaScript, Collections.singletonList(redisKey),
                    String.valueOf(limit), String.valueOf(windowSeconds));
            // 0=触发限流，1=正常
            return result == null || result == 0;

        } catch (Exception e) {
            // 熔断保护：Redis异常不影响主业务流程
            log.error("[限流异常] Redis执行失败, gatewayId:{}, err:{}", gatewayId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 异步查询网关综合授权信息（三级缓存架构）
     * 缓存查询顺序：本地缓存 -> Redis -> 数据库
     *
     * @param gatewayId 网关唯一标识
     * @param apiKey    认证密钥
     * @return 异步返回网关综合授权视图对象，无数据返回空Mono
     */
    @Override
    public Mono<McpGatewayCompositeVO> queryCompositeAuth(String gatewayId, String apiKey) {
        // 1. 核心参数校验，参数非法直接返回空
        if (StrUtil.isBlank(gatewayId) || StrUtil.isBlank(apiKey)) {
            return Mono.empty();
        }

        // 2. 统一构建RedisKey（带环境隔离）
        RedisKey.KeyDefinition authKeyDef = RedisKey.AUTH.build(gatewayId, apiKey);
        String cacheKey = authKeyDef.getKey();

        // 3. 一级缓存：本地缓存优先查询，性能最高
        McpGatewayCompositeVO localCacheVo = LOCAL_CACHE.getIfPresent(cacheKey);
        if (localCacheVo != null) {
            return NULL_VO.equals(localCacheVo) ? Mono.empty() : Mono.just(localCacheVo);
        }

        // 4. 异步加载：Redis二级缓存 + DB三级缓存
        return Mono.fromCallable(() -> {
                       try {
                           // ===================== 二级缓存：Redis =====================
                           McpGatewayCompositeVO redisVo = redisTemplate
                                   .opsForValue()
                                   .get(cacheKey);
                           if (redisVo != null) {
                               // 回写到本地缓存
                               LOCAL_CACHE.put(cacheKey, redisVo);
                               return redisVo;
                           }

                           // ===================== 三级缓存：数据库 =====================
                           McpGatewayCompositePO query = new McpGatewayCompositePO();
                           query.setGatewayId(gatewayId);
                           query.setApiKey(apiKey);

                           McpGatewayCompositePO compositePO = mcpGatewayAuthDao.queryCompositeAuth(query);
                           if (compositePO == null) {
                               // 空值缓存，防止缓存穿透
                               redisTemplate
                                       .opsForValue()
                                       .set(cacheKey, NULL_VO, NULL_CACHE_SECONDS, TimeUnit.SECONDS);
                               LOCAL_CACHE.put(cacheKey, NULL_VO);
                               return NULL_VO;
                           }

                           // ===================== 数据转换 & 双写缓存 =====================
                           McpGatewayCompositeVO resultVo = convertToVO(compositePO);
                           // 使用统一配置的TTL，避免硬编码
                           redisTemplate
                                   .opsForValue()
                                   .set(cacheKey, resultVo, authKeyDef
                                           .getTtl()
                                           .getSeconds(), TimeUnit.SECONDS);
                           LOCAL_CACHE.put(cacheKey, resultVo);

                           return resultVo;

                       } catch (Exception e) {
                           log.error("[鉴权查询异常] gatewayId:{}, apiKey:{}, err:{}", gatewayId, apiKey, e.getMessage(), e);
                           return NULL_VO;
                       }
                   })
                   // 异步调度，避免阻塞响应式流
                   .subscribeOn(Schedulers.boundedElastic())
                   .flatMap(vo -> NULL_VO.equals(vo) ? Mono.empty() : Mono.just(vo));
    }

    /**
     * 插入网关授权信息（事务控制 + 缓存一致性保障）
     *
     * @param vo 网关授权视图对象
     * @return 插入成功返回影响行数，失败返回0
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public int insert(McpGatewayAuthVO vo) {
        // 参数合法性校验
        if (vo == null || StrUtil.isBlank(vo.getGatewayId())) {
            log.warn("[授权插入] 参数非法");
            return 0;
        }

        // 生成/使用ApiKey，为空则自动生成
        String finalApiKey = StrUtil.isBlank(vo.getApiKey()) ? vo.generateKey() : vo.getApiKey();

        // 构建数据库持久化对象
        McpGatewayAuthPO po = McpGatewayAuthPO
                .builder()
                .gatewayId(vo.getGatewayId())
                .apiKey(finalApiKey)
                .rateLimit(vo.getRateLimit())
                .expireTime(vo.getExpireTime())
                .status(vo.getStatus() != null ? vo
                        .getStatus()
                        .getCode() : 1)
                .build();

        // 执行数据库插入
        int rows = mcpGatewayAuthDao.saveAuth(po);
        if (rows <= 0) {
            log.warn("[授权插入] 失败 gatewayId:{}", vo.getGatewayId());
            return 0;
        }

        // ===================== 缓存一致性清理 =====================
        try {
            String cacheKey = RedisKey.AUTH.getKey(vo.getGatewayId(), finalApiKey);
            // 先删Redis，再删本地缓存，保证数据一致性
            redisTemplate.delete(cacheKey);
            LOCAL_CACHE.invalidate(cacheKey);
            // 脱敏日志输出，保护敏感信息
            log.info("[授权插入] 成功 gatewayId:{}, apiKey:{}******", vo.getGatewayId(), StrUtil.sub(finalApiKey, 0, 6));
        } catch (Exception e) {
            log.error("[缓存清理失败] 不影响DB数据, gatewayId:{}", vo.getGatewayId(), e);
        }

        return rows;
    }

    /**
     * 查询网关有效授权数量
     *
     * @param gatewayId 网关唯一标识
     * @return 有效授权数量，参数非法返回0
     */
    @Override
    public int queryEffectiveGatewayAuthCount(String gatewayId) {
        if (StrUtil.isBlank(gatewayId)) {
            return 0;
        }
        return mcpGatewayAuthDao.queryEffectiveGatewayAuthCount(gatewayId);
    }

    /**
     * 查询网关有效授权信息
     *
     * @param commandEntity 许可证命令实体
     * @return 网关授权视图对象，无数据返回null
     */
    @Override
    public McpGatewayAuthVO queryEffectiveGatewayAuthInfo(LicenseCommandEntity commandEntity) {
        // 参数校验
        if (commandEntity == null || StrUtil.isBlank(commandEntity.getGatewayId()) || StrUtil.isBlank(commandEntity.getApiKey())) {
            return null;
        }

        // 数据库查询
        McpGatewayAuthPO po = mcpGatewayAuthDao.queryMcpGatewayAuthPO(McpGatewayAuthPO
                .builder()
                .gatewayId(commandEntity.getGatewayId())
                .apiKey(commandEntity.getApiKey())
                .build());

        if (po == null) {
            return null;
        }

        // PO转换为VO并返回
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
     * @return 网关鉴权状态枚举，参数非法/数据不存在返回默认状态
     */
    @Override
    public AuthStatusEnum.GatewayConfig queryGatewayAuthStatus(String gatewayId) {
        if (StrUtil.isBlank(gatewayId)) {
            return AuthStatusEnum.GatewayConfig.STRONG_VERIFIED;
        }

        McpGatewayPO po = mcpGatewayDao.queryByGatewayId(gatewayId);
        if (po == null) {
            return AuthStatusEnum.GatewayConfig.STRONG_VERIFIED;
        }

        return AuthStatusEnum.GatewayConfig.get(po.getAuth());
    }

    // ===================== 转换器 =====================

    /**
     * 数据库复合PO对象转换为视图VO对象
     *
     * @param po 网关授权复合持久化对象
     * @return 网关授权复合视图对象
     */
    private McpGatewayCompositeVO convertToVO(McpGatewayCompositePO po) {
        if (po == null) {
            return null;
        }
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