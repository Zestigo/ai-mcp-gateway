package com.c.trigger.listener;

import com.c.types.util.InstanceProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * Redis消息订阅配置类
 * 配置当前服务节点的Redis消息监听容器，绑定本机IP专属主题
 * 用于接收分布式环境中其他节点转发的MCP会话推送消息
 */
@Configuration
public class RedisSubConfig {

    /**
     * 注册Redis消息监听容器
     * 绑定当前节点IP对应的主题，实现定向消息接收与处理
     *
     * @param factory          Redis连接工厂
     * @param listenerAdapter  消息监听器适配器
     * @param instanceProvider 实例宿主信息提供者
     * @return Redis消息监听容器实例
     */
    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory factory, MessageListenerAdapter listenerAdapter,
                                            InstanceProvider instanceProvider) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);

        // 获取本机校验后的IP，构建专属订阅主题
        String myTopic = "mcp_node_" + instanceProvider.getHostIp();

        // 为容器绑定监听器与主题
        container.addMessageListener(listenerAdapter, new PatternTopic(myTopic));

        return container;
    }

    /**
     * 注册消息监听器适配器
     * 绑定消息接收处理器与默认执行方法，适配Redis消息监听规范
     *
     * @param receiver 自定义Redis消息接收监听器
     * @return 消息监听器适配器实例
     */
    @Bean
    MessageListenerAdapter listenerAdapter(McpRedisListener receiver) {
        // 指定监听器执行 onMessage 方法
        return new MessageListenerAdapter(receiver, "onMessage");
    }
}