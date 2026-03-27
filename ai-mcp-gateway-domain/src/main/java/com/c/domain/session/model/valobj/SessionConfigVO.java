package com.c.domain.session.model.valobj;

import com.c.domain.session.model.entity.McpSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

import java.time.Instant;

/**
 * 会话配置视图对象
 * 承载会话元数据、分布式寻址标识与会话状态信息
 * 用于会话上下文传递、分布式路由与会话配置管理
 *
 * @author cyh
 * @date 2026/03/27
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionConfigVO {

    /** 会话唯一标识 */
    private String sessionId;

    /** 网关唯一标识 */
    private String gatewayId;

    /** 宿主机IP标识，用于分布式场景下的消息路由寻址 */
    private String hostIp;

    /** 会话超时时间，单位：秒 */
    private Integer timeoutSeconds;

    /** 会话创建时间 */
    private Instant createTime;

    /** 会话最后访问时间 */
    private Instant lastAccessedTime;

    /** 会话是否有效状态 */
    private boolean active;

    /**
     * 会话消息推送Sink
     * transient修饰，避免序列化到数据库/Redis等持久化介质
     */
    private transient Sinks.Many<ServerSentEvent<String>> sink;

    /**
     * 从会话实体构建会话配置视图对象
     *
     * @param session 会话实体对象
     * @return 构建完成的会话配置视图对象
     */
    public static SessionConfigVO from(McpSession session) {
        return SessionConfigVO
                .builder()
                .sessionId(session.getSessionId())
                .gatewayId(session.getGatewayId())
                .hostIp(session.getHostIp())
                .timeoutSeconds(session.getTimeoutSeconds())
                .createTime(session.getCreateTime())
                .lastAccessedTime(session.getLastAccessTime())
                .active(session.isActive())
                .build();
    }

    /**
     * 从会话实体构建会话配置视图对象，并注入消息推送Sink
     *
     * @param session 会话实体对象
     * @param sink    服务端推送消息Sink
     * @return 注入Sink后的会话配置视图对象
     */
    public static SessionConfigVO from(McpSession session, Sinks.Many<ServerSentEvent<String>> sink) {
        SessionConfigVO vo = from(session);
        vo.sink = sink;
        return vo;
    }
}