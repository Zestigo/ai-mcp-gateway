package com.c.domain.protocol.service.analysis.strategy.impl;

import com.alibaba.fastjson2.JSONObject;
import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;
import com.c.domain.protocol.service.analysis.strategy.AbstractProtocolAnalysisStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

/**
 * 响应体解析策略实现类
 * 解析Swagger/OpenAPI接口文档中的responses节点，聚焦200成功响应，构建响应结果树状映射规则
 * 兼容OpenAPI 3.0与Swagger 2.0双规范，支持引用、数组、内联对象三种根节点类型
 *
 * @author cyh
 * @date 2026/03/29
 */
@Slf4j
@Component("responseAnalysis")
@Order(3)
public class ResponseAnalysisStrategy extends AbstractProtocolAnalysisStrategy {

    /**
     * 获取当前解析策略对应的映射类型标识
     */
    @Override
    protected String getMappingType() {
        return "response";
    }

    /**
     * 策略匹配判断方法
     * 校验接口操作节点是否包含responses属性，决定是否执行当前解析策略
     *
     * @param operation 接口操作节点JSON对象
     * @return 包含responses节点返回true，否则返回false
     */
    @Override
    public boolean match(JSONObject operation) {
        return operation != null && operation.containsKey("responses");
    }

    /**
     * 执行响应体解析核心逻辑
     * 定位成功响应节点，提取Schema信息，识别根节点类型并触发递归解析
     *
     * @param operation 接口操作节点，包含responses响应信息
     * @param schemas   全局模型定义池，用于解析$ref引用类型
     * @param mappings  解析结果映射集合，存储封装后的响应体映射信息
     */
    @Override
    public void doAnalysis(JSONObject operation, JSONObject schemas, List<HTTPProtocolVO.ProtocolMapping> mappings) {
        // 获取响应定义节点，优先使用200成功响应，无则使用default默认响应
        JSONObject responses = operation.getJSONObject("responses");
        if (responses == null) {
            return;
        }

        JSONObject successResponse = responses.getJSONObject("200");
        if (successResponse == null) {
            successResponse = responses.getJSONObject("default");
        }
        if (successResponse == null) {
            return;
        }

        // 兼容双规范提取响应体Schema节点
        JSONObject schema = extractResponseSchema(successResponse);
        if (schema == null) {
            return;
        }

        // 初始化根节点解析参数
        String rootName = null;
        JSONObject rootSchema = null;
        String rootType = "object";

        // 处理根节点为$ref引用类型
        if (schema.containsKey("$ref")) {
            String ref = schema.getString("$ref");
            String refName = ref.substring(ref.lastIndexOf('/') + 1);
            rootSchema = schemas.getJSONObject(refName);
            rootName = toLowerCamel(refName);
        }
        // 处理根节点为数组类型
        else if ("array".equalsIgnoreCase(schema.getString("type"))) {
            rootType = "array";
            JSONObject items = schema.getJSONObject("items");
            if (items != null && items.containsKey("$ref")) {
                String ref = items.getString("$ref");
                String refName = ref.substring(ref.lastIndexOf('/') + 1);
                rootSchema = schemas.getJSONObject(refName);
                rootName = toLowerCamel(refName);
            }
        }
        // 处理根节点为内联对象
        else if (schema.containsKey("properties")) {
            rootSchema = schema;
            rootName = "res";
        }

        // 根节点校验通过，执行解析流程
        if (rootSchema != null && rootName != null) {
            // 添加响应根节点映射记录
            addResponseRootMapping(rootName, rootSchema, mappings, rootType);

            // 递归解析所有子属性
            parseProperties(rootName, rootSchema.getJSONObject("properties"), rootSchema.getJSONArray("required"),
                    schemas, mappings, new HashSet<>());
        }
    }

    /**
     * 从响应节点中提取Schema，兼容双规范
     * OpenAPI 3.0：通过content->application/json->schema获取
     * Swagger 2.0：直接通过schema节点获取
     *
     * @param responseNode 响应节点JSON对象
     * @return 解析成功返回Schema对象，解析失败返回null
     */
    private JSONObject extractResponseSchema(JSONObject responseNode) {
        // OpenAPI 3.0规范解析逻辑
        if (responseNode.containsKey("content")) {
            JSONObject content = responseNode.getJSONObject("content");
            JSONObject appJson = content != null ? content.getJSONObject("application/json") : null;
            return appJson != null ? appJson.getJSONObject("schema") : null;
        }
        // Swagger 2.0规范解析逻辑
        return responseNode.getJSONObject("schema");
    }

    /**
     * 构建并添加响应体根节点映射记录
     * 统一封装根节点基础信息，默认标记为必填项
     *
     * @param rootName 根节点名称
     * @param schema   根节点Schema对象
     * @param mappings 映射结果集合
     * @param mcpType  根节点数据类型
     */
    private void addResponseRootMapping(String rootName, JSONObject schema,
                                        List<HTTPProtocolVO.ProtocolMapping> mappings, String mcpType) {
        String description = schema.getString("description");
        if (description == null) {
            description = "响应体根节点";
        }

        mappings.add(HTTPProtocolVO.ProtocolMapping
                .builder()
                .mappingType(getMappingType())
                .parentPath(null)
                .fieldName(rootName)
                .mcpPath(rootName)
                .mcpType(mcpType)
                .mcpDescription(description)
                .isRequired(1)
                .sortOrder(mappings.size() + 1)
                .build());
    }
}