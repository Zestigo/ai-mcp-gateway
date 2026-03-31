package com.c.api.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 网关配置综合请求对象
 * 统一封装网关配置、工具配置、协议配置、认证配置的请求参数
 *
 * @author cyh
 * @date 2026/03/31
 */
public class GatewayConfigRequestDTO {

    /**
     * 网关基础配置请求参数
     */
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

        /** 协议业务版本号 */
        private String gatewayVersion;

        /** 认证状态，0-禁用，1-启用 */
        private Integer auth;

        /** 网关运行状态，0-禁用，1-启用 */
        private Integer status;
    }

    /**
     * 网关工具配置请求参数
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GatewayToolConfig {

        /** 所属网关唯一标识 */
        private String gatewayId;

        /** 工具唯一标识 */
        private Long toolId;

        /** MCP工具名称 */
        private String toolName;

        /** 工具类型，可选值：function/resource */
        private String toolType;

        /** 工具描述信息，非空校验 */
        @NotBlank(message = "工具描述不能为空")
        private String toolDescription;

        /** 工具版本号 */
        private String toolVersion;

        /** 关联协议ID */
        private Long protocolId;

        /** 协议类型 */
        private String protocolType;

        /** 工具状态，0-禁用，1-启用 */
        private Integer toolStatus;
    }

    /**
     * 网关协议配置请求参数
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GatewayProtocol {
        /** HTTP协议配置列表 */
        private List<HTTPProtocol> httpProtocols;

        /**
         * HTTP协议详细配置请求参数
         */
        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class HTTPProtocol {

            /** 协议唯一标识 */
            private Long protocolId;

            /** HTTP请求地址 */
            private String httpUrl;

            /** HTTP请求头，JSON字符串格式 */
            private String httpHeaders;

            /** HTTP请求方法 */
            private String httpMethod;

            /** 请求超时时间，单位：毫秒 */
            private Integer timeout;

            /** 参数映射配置列表 */
            private List<ProtocolMapping> mappings;
        }

        /**
         * 协议参数映射配置请求参数
         */
        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class ProtocolMapping {

            /** 映射类型，可选值：request/response */
            private String mappingType;

            /** 父级路径 */
            private String parentPath;

            /** 字段名称 */
            private String fieldName;

            /** MCP完整路径，非空校验 */
            @NotBlank(message = "映射路径不能为空")
            private String mcpPath;

            /** MCP数据类型，非空校验 */
            @NotBlank(message = "MCP类型不能为空")
            private String mcpType;

            /** MCP字段描述 */
            private String description;

            /** 是否必填，0-否，1-是 */
            private Integer isRequired;

            /** 排序顺序，数值越小优先级越高 */
            private Integer sortOrder;
        }
    }

    /**
     * 网关认证配置请求参数
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GatewayAuth {

        /** 网关唯一标识 */
        private String gatewayId;

        /** 访问速率限制，每小时最大访问次数 */
        private Integer rateLimit;

        /** API密钥过期时间 */
        private Date expireTime;

        /** 认证状态，0-禁用，1-启用 */
        private Integer status;
    }

}