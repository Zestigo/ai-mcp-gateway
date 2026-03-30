package com.c.api.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 会话创建请求
 *
 * @author cyh
 * @date 2026/03/24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpSessionRequestDTO {

    /** 网关唯一标识 */
    private String gatewayId;

    /** 认证密钥（预留扩展） */
    private String apiKey;

    /** 客户端类型（如 CLAUDE、IDE_PLUGIN、CUSTOM） */
    private String clientType;

    /** 会话超时时间（秒） */
    private Integer timeout;
}