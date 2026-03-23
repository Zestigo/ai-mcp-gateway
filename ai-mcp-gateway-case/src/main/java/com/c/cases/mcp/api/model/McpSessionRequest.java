package com.c.cases.mcp.api.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * MCP 会话创建请求对象
 * 用于接收客户端创建会话时的请求参数，封装网关标识、认证信息、客户端类型与会话超时配置
 *
 * @author cyh
 * @date 2026/03/24
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

    /** 客户端类型（如 CLAUDE、IDE_PLUGIN、CUSTOM） */
    private String clientType;

    /** 会话超时时间（秒） */
    private Integer timeout;
}