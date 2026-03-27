package com.c.trigger.listener;

import com.alibaba.fastjson2.JSON;
import com.c.domain.session.service.message.SessionMessageService;
import com.c.domain.session.service.message.SessionMessageServiceImpl.RemotePushMessage;
import cn.hutool.core.net.NetUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class McpRedisListener {

    private final SessionMessageService sessionMessageService;

    /**
     * 收到 Redis 广播消息后的回调
     */
    public void onMessage(String jsonMessage) {
        try {
            RemotePushMessage remoteMsg = JSON.parseObject(jsonMessage, RemotePushMessage.class);
            log.info("【Redis监听】收到跨机推送指令: sessionId={}", remoteMsg.getSessionId());
            
            // 调用本地 push，此时 getLocalSink(sessionId) 必然能命中
            sessionMessageService.push(remoteMsg.getSessionId(), remoteMsg.getMessage()).subscribe();
        } catch (Exception e) {
            log.error("解析跨机消息失败", e);
        }
    }
}
