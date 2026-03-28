package com.c.trigger.listener;

import com.alibaba.fastjson2.JSON;
import com.c.domain.auth.model.valobj.McpRemoteMessageVO;
import com.c.domain.session.service.message.SessionMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;

/**
 * Redis消息监听器，接收跨节点MCP消息并执行推送
 *
 * @author cyh
 * @date 2026/03/27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpRedisListener implements MessageListener {

    /** 会话消息服务，执行消息推送核心逻辑 */
    private final SessionMessageService sessionMessageService;

    /**
     * 接收Redis订阅消息，解析并触发会话推送
     *
     * @param message Redis消息体
     * @param pattern 订阅匹配规则
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // 将消息体转换为UTF-8字符串
            String json = new String(message.getBody(), StandardCharsets.UTF_8);

            // 解析为远程消息对象
            McpRemoteMessageVO remoteMsg = JSON.parseObject(json, McpRemoteMessageVO.class);
            if (remoteMsg == null) return;

            log.info("【跨机接应】监听成功, sessionId: {}", remoteMsg.getSessionId());

            // 异步调用领域服务执行消息推送
            sessionMessageService
                    .push(remoteMsg.getSessionId(), remoteMsg.getMessage())
                    .subscribeOn(Schedulers.parallel())
                    .doOnError(e -> log.error("【跨机推送失败】", e))
                    .subscribe();

        } catch (Exception e) {
            log.error("【Redis监听异常】解析失败", e);
        }
    }
}