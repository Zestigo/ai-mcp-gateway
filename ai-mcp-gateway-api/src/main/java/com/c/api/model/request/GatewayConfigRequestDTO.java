package com.c.api.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 网关配置请求对象
 *
 * @author cyh
 * @date 2026/03/29
 */
public class GatewayConfigRequestDTO {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GatewayConfig {

        /** 网关唯一标识 */
        private String gatewayId;

        /** 网关名称 */
        private String gatewayName;

        /** 网关描述 */
        private String gatewayDescription;

        /** 协议版本 */
        private String gatewayVersion;

        /** 校验状态：0-禁用，1-启用 */
        private Integer auth;

        /** 网关状态：0-不校验，1-强校验 */
        private Integer status;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GatewayToolConfig {

        /** 所属网关ID */
        private String gatewayId;

        /** 工具ID */
        private Long toolId;

        /** MCP工具名称 */
        private String toolName;

        /** 工具类型：function/resource */
        private String toolType;

        /** 工具描述 */
        @NotBlank(message = "工具描述不能为空")
        private String toolDescription;

        /** 工具版本 */
        private String toolVersion;

        /** 协议ID */
        private Long protocolId;

        /** 协议类型：http、dubbo、rabbitmq */
        private String protocolType;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GatewayProtocol {
        /** 协议列表数据 */
        private List<HTTPProtocol> httpProtocols;

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class HTTPProtocol {

            /** 协议ID */
            private Long protocolId;

            /** HTTP请求地址 */
            private String httpUrl;

            /** HTTP请求头 */
            private String httpHeaders;

            /** HTTP请求方法 */
            private String httpMethod;

            /** 超时时间 */
            private Integer timeout;

            /** 参数映射配置 */
            private List<ProtocolMapping> mappings;
        }

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class ProtocolMapping {

            /** 映射类型：request/response */
            private String mappingType;

            /** 父级路径 */
            private String parentPath;

            /** 字段名称 */
            private String fieldName;

            /** MCP完整路径 */
            @NotBlank(message = "映射路径不能为空")
            private String mcpPath;

            /** MCP数据类型 */
            @NotBlank(message = "MCP类型不能为空")
            private String mcpType;

            /** MCP字段描述 */
            private String Description;

            /** 是否必填：0-否，1-是 */
            private Integer isRequired;

            /** 排序顺序 */
            private Integer sortOrder;
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GatewayAuth {

        /** 网关ID */
        private String gatewayId;

        /** 速率限制 */
        private Integer rateLimit;

        /** 过期时间 */
        private Date expireTime;
    }

}