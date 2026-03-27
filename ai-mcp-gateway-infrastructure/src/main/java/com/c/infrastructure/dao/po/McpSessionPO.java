package com.c.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * MCP 会话数据库持久化对象
 *
 * @author cyh
 * @date 2026/03/27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpSessionPO {

    /** 数据库自增主键 */
    private Long id;

    /** 业务唯一会话ID */
    private String sessionId;

    /** 关联网关ID */
    private String gatewayId;

    /** 会话所在宿主机IP */
    private String hostIp;

    /** 活跃状态：1-活跃 0-失效 */
    private Integer active = 1; // 默认为 1

    /** 会话超时时间，单位秒 */
    private Integer timeoutSeconds;

    /** 最后访问时间 */
    private Date lastAccessTime;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}