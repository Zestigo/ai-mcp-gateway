package com.c.infrastructure.adapter;

import com.c.domain.session.adapter.repository.SessionRedisPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

/**
 * 会话Redis存储适配器
 *
 * @author cyh
 * @date 2026/03/24
 */
@Service
@RequiredArgsConstructor
public class SessionRedisAdapter implements SessionRedisPort {

    /** Redis模板 */
    private final StringRedisTemplate redisTemplate;

    /** Redis键前缀 */
    private static final String PREFIX = "mcp:gateway:";

    /**
     * 绑定网关与会话
     *
     * @param gatewayId 网关标识
     * @param sessionId 会话标识
     */
    @Override
    public void bindSession(String gatewayId, String sessionId) {
        // 使用Set存储网关下的会话
        redisTemplate
                .opsForSet()
                .add(PREFIX + gatewayId, sessionId);
    }

    /**
     * 获取网关下所有会话
     *
     * @param gatewayId 网关标识
     * @return 会话ID集合
     */
    @Override
    public Set<String> getSessions(String gatewayId) {
        Set<String> set = redisTemplate
                .opsForSet()
                .members(PREFIX + gatewayId);
        return set == null ? Collections.emptySet() : set;
    }

    /**
     * 移除网关与会话关联
     *
     * @param gatewayId 网关标识
     * @param sessionId 会话标识
     */
    @Override
    public void removeSession(String gatewayId, String sessionId) {
        redisTemplate
                .opsForSet()
                .remove(PREFIX + gatewayId, sessionId);
    }
}