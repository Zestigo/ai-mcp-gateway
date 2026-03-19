package com.c.cases.mcp.api.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * MCP会话创建请求对象
 *
 * @author cyh
 * @date 2026/03/18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpSessionRequest {

    /** 网关唯一标识 */
    private String gatewayId;

    /** 认证密钥（预留扩展） */
    private String apiKey;

    /** 客户端类型（如CLAUDE、IDE_PLUGIN、CUSTOM） */
    private String clientType;

    /** 会话超时时间（秒） */
    private Integer timeout;
}