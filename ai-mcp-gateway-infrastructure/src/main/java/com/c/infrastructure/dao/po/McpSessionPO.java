package com.c.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * MCP 会话持久化对象
 *
 * @author cyh
 * @date 2026/03/25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpSessionPO {

    /** 会话唯一标识 */
    private String sessionId;

    /** 关联网关ID */
    private String gatewayId;

    /** 是否激活 1-活跃 0-失效 */
    private Integer active;

    /** 超时时间（秒） */
    private Integer timeoutSeconds;

    /** 最后访问时间 */
    private Date lastAccessTime;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}