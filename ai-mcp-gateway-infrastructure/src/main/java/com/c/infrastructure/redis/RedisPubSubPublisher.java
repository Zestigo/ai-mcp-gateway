package com.c.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisPubSubPublisher {

    private final StringRedisTemplate redisTemplate;

    public void publish(String sessionId, String msg) {
        redisTemplate.convertAndSend("mcp:session:" + sessionId, msg);
    }
}