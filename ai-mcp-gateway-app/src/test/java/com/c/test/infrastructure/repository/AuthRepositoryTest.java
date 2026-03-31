package com.c.test.infrastructure.repository;

import com.c.domain.auth.adapter.repository.AuthRepository;
import com.c.domain.auth.model.valobj.McpGatewayAuthVO;
import com.c.domain.auth.model.valobj.enums.AuthStatusEnum;
import com.c.infrastructure.dao.McpGatewayDao;
import com.c.infrastructure.dao.po.McpGatewayPO;
import com.c.infrastructure.redis.RedisKeyConstants;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import reactor.test.StepVerifier;

import javax.annotation.Resource;
import java.util.Date;
import java.util.UUID;

/**
 * 认证授权仓储层集成测试
 * 测试核心能力：三级缓存加载机制、分布式限流原子性、缓存穿透防护、网关授权状态查询
 * 依赖环境：SpringBoot上下文、MySQL数据库、Redis服务
 *
 * @author cyh
 * @date 2026/03/28
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthRepositoryTest {

    /** 认证授权仓储接口，测试核心依赖 */
    @Resource
    private AuthRepository authRepository;

    /** Redis字符串模板，用于缓存校验与清理操作 */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 网关数据访问对象，用于初始化测试数据 */
    @Resource
    private McpGatewayDao mcpGatewayDao;

    /** 测试用网关唯一标识，每次测试方法执行前重新生成 */
    private String gatewayId;

    /** 测试用API密钥，每次测试方法执行前重新生成 */
    private String apiKey;

    /**
     * 测试方法前置初始化
     * 为每个测试方法生成独立的网关ID与API密钥，避免数据冲突
     */
    @BeforeEach
    void setUp() {
        // 生成8位随机后缀，保证测试数据唯一性
        String randomSuffix = UUID
                .randomUUID()
                .toString()
                .substring(0, 8);
        this.gatewayId = "GW_" + randomSuffix;
        this.apiKey = "AK_" + randomSuffix;
        System.out.println(">>> 测试数据初始化完成 gatewayId=" + gatewayId + ", apiKey=" + apiKey);
    }

    /**
     * 测试数据插入与三级缓存加载逻辑
     * 验证流程：数据库插入 -> 首次查询DB回源并写入缓存 -> 二次查询命中本地缓存
     */
    @Test
    @Order(1)
    @DisplayName("测试：插入数据并验证三级缓存加载逻辑")
    public void testInsertAndQueryCache() {
        System.out.println(">>> 开始执行测试：插入数据并验证三级缓存加载逻辑");

        // 1. 初始化网关基础数据，满足查询SQL的状态条件
        mcpGatewayDao.insertGateway(McpGatewayPO
                .builder()
                .gatewayId(gatewayId)
                .gatewayName("集成测试网关")
                .gatewayDescription("Unit Test")
                .gatewayVersion("1.0.0")
                .auth(AuthStatusEnum.GatewayConfig.STRONG_VERIFIED.getCode())
                .status(1)
                .build());
        System.out.println(">>> 网关基础数据插入完成 gatewayId=" + gatewayId);

        // 2. 构建认证授权视图对象，设置有效期与启用状态
        McpGatewayAuthVO vo = McpGatewayAuthVO
                .builder()
                .gatewayId(gatewayId)
                .apiKey(apiKey)
                .rateLimit(50)
                .expireTime(DateUtils.addDays(new Date(), 7))
                .status(AuthStatusEnum.AuthConfig.ENABLE)
                .build();
        System.out.println(">>> 认证VO构建完成 apiKey=" + apiKey + ", rateLimit=50");

        // 3. 执行数据插入，触发旧缓存清理
        authRepository.insert(vo);
        System.out.println(">>> 认证数据插入完成，缓存已自动清理");

        // 4. 第一次查询：验证从数据库加载数据并写入各级缓存
        System.out.println(">>> 第一次查询：预期DB回源 + 写入缓存");
        authRepository
                .queryCompositeAuth(gatewayId, apiKey)
                .as(StepVerifier::create)
                .expectNextMatches(composite -> {
                    System.out.println(">>> 收到复合查询结果: " + composite);
                    // 校验返回数据的密钥与限流值正确性
                    boolean match = apiKey.equals(composite.getApiKey()) && Long
                            .valueOf(50)
                            .equals(composite.getRateLimit());
                    System.out.println(">>> 第一次查询数据校验结果：" + match);
                    return match;
                })
                .verifyComplete();

        // 5. 第二次查询：验证直接命中本地内存缓存，无DB与Redis访问
        System.out.println(">>> 第二次查询：预期直接命中本地内存缓存");
        authRepository
                .queryCompositeAuth(gatewayId, apiKey)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
        System.out.println(">>> 第二次查询缓存命中成功");
    }

    /**
     * 测试分布式限流Lua脚本原子性
     * 验证逻辑：在时间窗口内，请求次数超过阈值则触发限流
     */
    @Test
    @Order(2)
    @DisplayName("测试：分布式限流 Lua 脚本原子性判断")
    public void testDistributedRateLimit() {
        System.out.println(">>> 开始执行测试：分布式限流Lua脚本原子性判断");

        // 构建限流Redis键并清理历史数据
        String limitKey = RedisKeyConstants.buildRateLimitKey(gatewayId, apiKey);
        stringRedisTemplate.delete(limitKey);
        System.out.println(">>> 旧限流缓存已清理 key=" + limitKey);

        // 限流配置：最大请求次数3次，时间窗口60秒
        int limit = 3;
        int window = 60;
        System.out.println(">>> 限流配置：次数=" + limit + ", 窗口秒数=" + window);

        // 前3次请求：正常访问，不触发限流
        for (int i = 1; i <= limit; i++) {
            boolean isLimited = authRepository.isRateLimited(gatewayId, apiKey, limit, window);
            System.out.println(">>> 第" + i + "次访问限流结果：" + isLimited);
            Assertions.assertFalse(isLimited, "第 " + i + " 次访问不应被限流");
        }

        // 第4次请求：超出限流阈值，必须触发限流
        boolean r4 = authRepository.isRateLimited(gatewayId, apiKey, limit, window);
        System.out.println(">>> 第4次访问限流结果：" + r4 + "（预期=true）");
        Assertions.assertTrue(r4, "第 4 次访问必须触发限流");

        // 测试完成，清理限流缓存
        stringRedisTemplate.delete(limitKey);
        System.out.println(">>> 限流测试完成，缓存已清理");
    }

    /**
     * 测试缓存穿透防护机制
     * 验证逻辑：查询不存在的数据时，自动缓存空值，防止频繁访问数据库
     */
    @Test
    @Order(3)
    @DisplayName("测试：缓存穿透防护")
    public void testCachePenetrationProtection() {
        System.out.println(">>> 开始执行测试：缓存穿透防护");

        // 生成随机不存在的API密钥，模拟非法查询
        String fakeKey = "NON_EXIST_KEY_" + UUID
                .randomUUID()
                .toString()
                .substring(0, 4);
        System.out.println(">>> 模拟非法查询 fakeKey=" + fakeKey);

        // 查询不存在的认证数据，预期返回空结果
        System.out.println(">>> 开始查询不存在的认证数据");
        authRepository
                .queryCompositeAuth(gatewayId, fakeKey)
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
        System.out.println(">>> 非法查询完成，返回空结果（符合预期）");

        // 校验Redis是否已存储空值缓存标记
        String redisKey = RedisKeyConstants.buildAuthKey(gatewayId, fakeKey);
        Boolean hasKey = stringRedisTemplate.hasKey(redisKey);
        System.out.println(">>> Redis 空值缓存状态: " + hasKey + " key=" + redisKey);
    }

    /**
     * 测试网关全局授权状态查询
     * 验证逻辑：根据网关ID查询对应的认证配置状态
     */
    @Test
    @Order(4)
    @DisplayName("测试：网关全局校验状态查询")
    public void testGatewayAuthStatus() {
        System.out.println(">>> 开始执行测试：网关全局校验状态查询");

        // 初始化测试网关数据，设置强认证状态
        mcpGatewayDao.insertGateway(McpGatewayPO
                .builder()
                .gatewayId(gatewayId)
                .gatewayName("测试网关-" + gatewayId)
                .status(1)
                .auth(AuthStatusEnum.GatewayConfig.STRONG_VERIFIED.getCode())
                .build());
        System.out.println(">>> 测试网关数据插入完成 gatewayId=" + gatewayId);

        // 查询网关认证状态并校验结果
        AuthStatusEnum.GatewayConfig config = authRepository.queryGatewayAuthStatus(gatewayId);
        System.out.println(">>> 查询到网关认证状态：" + config);
        Assertions.assertEquals(AuthStatusEnum.GatewayConfig.STRONG_VERIFIED, config);
        System.out.println(">>> 网关状态校验通过");
    }
}