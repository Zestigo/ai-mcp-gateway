package com.c.domain.session.model.valobj;

import com.c.domain.session.model.entity.McpSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 会话视图对象：只承载元数据，不持有 Sink
 *
 * @author cyh
 * @date 2026/03/24
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
    /** 超时时间（秒） */
    private Integer timeoutSeconds;
    /** 创建时间 */
    private Instant createTime;
    /** 最后访问时间 */
    private Instant lastAccessedTime;
    /** 是否有效 */
    private boolean active;

    /**
     * 从会话实体构建视图对象
     *
     * @param session 会话实体
     * @return 会话视图对象
     */
    public static SessionConfigVO from(McpSession session) {
        // 从实体拷贝核心元数据
        return SessionConfigVO
                .builder()
                .sessionId(session.getSessionId())
                .gatewayId(session.getGatewayId())
                .timeoutSeconds(session.getTimeoutSeconds())
                .createTime(session.getCreateTime())
                .lastAccessedTime(session.getLastAccessTime())
                .active(session.isActive())
                .build();
    }
}