package com.c.domain.session.model.valobj.gateway;

import lombok.*;

/**
 * MCP网关协议配置值对象
 * 封装网关调用所需的各类协议配置信息
 *
 * @author cyh
 * @date 2026/03/25
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class McpGatewayProtocolConfigVO {

    /** 网关唯一标识 */
    private String gatewayId;

    /** HTTP协议配置 */
    private HTTPConfig httpConfig;

    /**
     * HTTP请求配置信息
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HTTPConfig {

        /** 请求地址 */
        private String httpUrl;

        /** 请求头（JSON 字符串） */
        private String httpHeaders;

        /** 请求方法（GET/POST） */
        private String httpMethod;

        /** 超时时间（毫秒） */
        private Integer timeout;
    }
}