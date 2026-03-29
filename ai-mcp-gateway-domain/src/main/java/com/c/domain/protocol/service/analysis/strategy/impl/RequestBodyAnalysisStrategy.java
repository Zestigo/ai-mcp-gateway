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
 * 请求体解析策略实现类
 * 解析Swagger/OpenAPI接口文档中的requestBody节点，构建JSON格式的请求体树状映射规则
 * 支持引用类型、内联对象、数组三种根节点形态，通过DFS递归解析嵌套属性
 *
 * @author cyh
 * @date 2026/03/29
 */
@Slf4j
@Component("requestBodyAnalysis")
@Order(1)
public class RequestBodyAnalysisStrategy extends AbstractProtocolAnalysisStrategy {

    /**
     * 获取当前解析策略对应的映射类型标识
     */
    @Override
    protected String getMappingType() {
        return "request";
    }

    /**
     * 策略匹配判断方法
     * 校验接口操作节点是否包含requestBody属性，决定是否执行当前解析策略
     *
     * @param operation OpenAPI接口操作节点JSON对象
     * @return 包含requestBody节点返回true，否则返回false
     */
    @Override
    public boolean match(JSONObject operation) {
        return operation != null && operation.containsKey("requestBody");
    }

    /**
     * 执行请求体解析核心逻辑
     * 提取JSON格式的Schema节点，识别根节点类型，初始化解析上下文并触发递归解析
     *
     * @param operation 接口操作节点，包含requestBody请求体信息
     * @param schemas   全局模型定义池，用于解析$ref引用类型
     * @param mappings  解析结果映射集合，存储封装后的请求体映射信息
     */
    @Override
    public void doAnalysis(JSONObject operation, JSONObject schemas, List<HTTPProtocolVO.ProtocolMapping> mappings) {
        // 安全提取application/json类型对应的Schema节点
        JSONObject schema = getSchemaNode(operation);
        if (schema == null) {
            return;
        }

        // 初始化根节点解析参数
        String rootName = null;
        JSONObject rootSchema = null;
        String rootType = "object";

        // 处理根节点为$ref引用类型的场景
        if (schema.containsKey("$ref")) {
            String ref = schema.getString("$ref");
            String refName = ref.substring(ref.lastIndexOf('/') + 1);
            rootSchema = schemas.getJSONObject(refName);
            rootName = toLowerCamel(refName);
        }
        // 处理根节点为内联对象的场景
        else if (schema.containsKey("properties")) {
            rootSchema = schema;
            rootName = "req";
        }
        // 处理根节点为数组类型的场景
        else if ("array".equalsIgnoreCase(schema.getString("type"))) {
            rootType = "array";
            JSONObject items = schema.getJSONObject("items");
            if (items != null) {
                // 数组元素为引用类型
                if (items.containsKey("$ref")) {
                    String ref = items.getString("$ref");
                    String refName = ref.substring(ref.lastIndexOf('/') + 1);
                    rootSchema = schemas.getJSONObject(refName);
                    rootName = toLowerCamel(refName);
                }
                // 数组元素为内联对象/基本类型
                else {
                    rootSchema = items;
                    rootName = "items";
                }
            }
        }

        // 根节点校验通过，执行解析流程
        if (rootSchema != null && rootName != null) {
            // 添加根节点映射记录，构建完整层级起点
            addRootMapping(rootName, rootSchema, mappings, rootType);

            // 深度优先递归解析所有子属性
            parseProperties(rootName, rootSchema.getJSONObject("properties"), rootSchema.getJSONArray("required"),
                    schemas, mappings, new HashSet<>());
        }
    }

    /**
     * 安全获取请求体中JSON格式对应的Schema节点
     * 逐层解析requestBody->content->application/json->schema结构
     *
     * @param operation 接口操作节点JSON对象
     * @return 解析成功返回Schema对象，解析失败返回null
     */
    private JSONObject getSchemaNode(JSONObject operation) {
        JSONObject requestBody = operation.getJSONObject("requestBody");
        if (requestBody == null) {
            return null;
        }

        JSONObject content = requestBody.getJSONObject("content");
        if (content == null) {
            return null;
        }

        JSONObject appJson = content.getJSONObject("application/json");
        return appJson != null ? appJson.getJSONObject("schema") : null;
    }

    /**
     * 构建并添加请求体根节点的映射记录
     * 根节点无父级路径，默认标记为必填项
     *
     * @param rootName 根节点名称
     * @param schema   根节点对应的Schema对象
     * @param mappings 映射结果集合
     * @param mcpType  根节点数据类型
     */
    private void addRootMapping(String rootName, JSONObject schema, List<HTTPProtocolVO.ProtocolMapping> mappings,
                                String mcpType) {
        // 优先使用description，无则使用title
        String description = schema.getString("description");
        if (description == null) {
            description = schema.getString("title");
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