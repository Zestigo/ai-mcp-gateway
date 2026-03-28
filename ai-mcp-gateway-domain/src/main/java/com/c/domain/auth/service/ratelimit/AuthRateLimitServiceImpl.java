package com.c.domain.auth.service.ratelimit;

import com.c.domain.auth.adapter.repository.AuthRepository;
import com.c.domain.auth.model.entity.LicenseCommandEntity;
import com.c.domain.auth.model.entity.RateLimitCommandEntity;
import com.c.domain.auth.model.valobj.McpGatewayAuthVO;
import com.c.domain.auth.service.AuthRateLimitService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 分布式调用限制服务实现类
 * 遵循DDD架构规范，仅负责限流业务逻辑编排，具体Redis/Lua限流实现由仓储层提供
 * 实现API密钥维度的小时级访问频率控制，防止接口滥用
 *
 * @author cyh
 * @date 2026/03/27
 */
@Slf4j
@Service
public class AuthRateLimitServiceImpl implements AuthRateLimitService {

    /** 认证授权仓储接口，提供限流配置查询与分布式计数能力 */
    @Resource
    private AuthRepository repository;

    /**
     * 执行API访问限流校验
     * 空密钥直接放行，有效密钥查询限流配置，无配置则放行，有配置执行分布式限流判断
     *
     * @param commandEntity 限流校验命令实体，封装网关标识与API密钥信息
     * @return 响应式布尔结果，true表示触发限流拦截，false表示放行请求
     */
    @Override
    public Mono<Boolean> rateLimit(RateLimitCommandEntity commandEntity) {
        // 获取限流校验参数
        String gatewayId = commandEntity.getGatewayId();
        String apiKey = commandEntity.getApiKey();

        // 空密钥场景直接放行，不做限流控制
        if (apiKey == null || apiKey.isEmpty()) return Mono.just(true);

        return Mono
                .fromCallable(() -> {
                    // 1. 双层缓存查询API密钥对应的限流配置信息
                    McpGatewayAuthVO vo = repository.queryEffectiveGatewayAuthInfo(new LicenseCommandEntity(gatewayId
                            , apiKey));

                    // 2. 配置校验：未配置限流参数或限流阈值无效，直接放行
                    if (vo == null || vo.getRateLimit() == null || vo.getRateLimit() <= 0) {
                        return false;
                    }

                    // 3. 执行分布式限流：按小时维度统计访问次数，判断是否超出上限
                    boolean isLimited = repository.isRateLimited(gatewayId, apiKey, vo.getRateLimit(), 3600);

                    // 限流触发时记录告警日志
                    if (isLimited) {
                        log.warn("[限流拦截] 已达到访问上限: {}/h, Gateway: {}, Key: {}", vo.getRateLimit(), gatewayId, apiKey);
                    }
                    return isLimited;

                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}