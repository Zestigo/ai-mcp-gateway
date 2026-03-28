package com.c.domain.auth.service;

import com.c.domain.auth.model.entity.RateLimitCommandEntity;
import reactor.core.publisher.Mono;

/**
 * 接口限流服务接口
 * 定义网关请求限流控制标准能力，提供响应式限流校验接口
 *
 * @author cyh
 * @date 2026/03/27
 */
public interface AuthRateLimitService {

    /**
     * 执行限流校验
     * true - 已触发限流
     * false - 未触发限流
     *
     * @param commandEntity 限流校验命令实体
     * @return 响应式限流结果
     */
    Mono<Boolean> rateLimit(RateLimitCommandEntity commandEntity);

}