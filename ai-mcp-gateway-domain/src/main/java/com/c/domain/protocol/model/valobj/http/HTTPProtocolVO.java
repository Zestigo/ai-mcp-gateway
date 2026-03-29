package com.c.domain.protocol.model.valobj.http;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * HTTP协议值对象
 * 封装完整的HTTP接口元数据，作为协议解析结果载体，支撑网关动态调用
 *
 * @author cyh
 * @date 2026/03/28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HTTPProtocolVO {

    /** HTTP请求完整地址 */
    private String httpUrl;

    /** HTTP请求头，标准JSON字符串格式 */
    private String httpHeaders;

    /** HTTP请求方法，小写格式 */
    private String httpMethod;

    /** 接口调用超时时间，单位毫秒，默认30000 */
    @Builder.Default
    private Integer timeout = 30000;

    /** 协议参数映射规则集合 */
    private List<ProtocolMapping> mappings;

    /**
     * 协议参数映射配置类
     * 定义接口字段与MCP协议模型的双向映射规则
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProtocolMapping {
        /** 映射类型，枚举值：request-请求参数映射，response-响应数据映射，用于区分当前字段属于请求还是响应场景 */
        private String mappingType;

        /** 父级节点路径，示例：xxxRequest01，用于构建字段嵌套层级结构，根节点/顶级字段时为NULL */
        private String parentPath;

        /** 业务字段名称，示例：city、company、name，对应接口/数据模型中的原始字段名 */
        private String fieldName;

        /** MCP全路径，由父级路径+当前字段拼接而成，示例：xxxRequest01.city、xxxRequest01.company.name */
        private String mcpPath;

        /** MCP数据类型，固定枚举值：string/number/boolean/object/array，定义字段的基础数据格式 */
        private String mcpType;

        /** MCP字段描述信息，用于说明字段的业务含义、使用场景等说明性内容 */
        private String mcpDescription;

        /** 是否必填字段，枚举值：0-非必填，1-必填，用于生成接口文档中的required校验数组 */
        @Builder.Default
        private Integer isRequired = 0;

        /** 字段排序序号，仅作用于同一层级下的字段展示/处理顺序，数值越小排序越靠前 */
        private Integer sortOrder;
    }
}