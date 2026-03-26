package com.c.domain.session.model.valobj.gateway;

import lombok.*;

import java.util.List;

/**
 * MCP工具协议配置值对象
 * 封装HTTP请求配置与字段映射规则
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class McpToolProtocolConfigVO {

    /** HTTP请求配置信息 */
    private HTTPConfig httpConfig;

    /** 请求参数协议映射列表 */
    private List<ProtocolMapping> requestProtocolMappings;

    /**
     * HTTP请求配置
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HTTPConfig {
        /** 请求地址 */
        private String httpUrl;
        /** 请求头信息 */
        private String httpHeaders;
        /** 请求方法 */
        private String httpMethod;
        /** 超时时间 */
        private Integer timeout;
    }

    /**
     * 协议字段映射配置
     */
    @Getter
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
        private String mcpPath;
        /** MCP数据类型 */
        private String mcpType;
        /** 字段描述 */
        private String McpDescription;
        /** 是否必填 */
        private Integer isRequired;
        /** 排序序号 */
        private Integer sortOrder;
    }

}