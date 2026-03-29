package com.c.domain.protocol.service.analysis.strategy.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;
import com.c.domain.protocol.service.analysis.strategy.AbstractProtocolAnalysisStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Parameters参数解析策略实现类
 * 解析Swagger/OpenAPI接口文档中的parameters节点，提取并封装query、path类型的请求参数映射信息
 * 支持OpenAPI 3.0与Swagger 2.0双规范兼容解析，生成平铺结构的参数映射关系
 *
 * @author cyh
 * @date 2026/03/29
 */
@Slf4j
@Component("parametersAnalysisStrategy")
@Order(2)
public class ParametersAnalysisStrategy extends AbstractProtocolAnalysisStrategy {

    /**
     * 获取当前解析策略对应的映射类型标识
     */
    @Override
    protected String getMappingType() {
        return "request";
    }

    /**
     * 策略匹配判断方法
     * 校验接口操作节点是否包含parameters属性，决定是否执行当前解析策略
     *
     * @param operation 接口操作节点JSON对象
     * @return 匹配返回true，不匹配返回false
     */
    @Override
    public boolean match(JSONObject operation) {
        return operation != null && operation.containsKey("parameters");
    }

    /**
     * 执行Parameters参数解析核心逻辑
     * 遍历参数数组，过滤出query/path类型参数，提取参数元数据并构建映射对象存入集合
     *
     * @param operation 接口操作节点，包含parameters参数数组
     * @param schemas   全局模型定义集合，用于兼容复杂引用场景
     * @param mappings  解析结果映射集合，存储最终封装的参数映射信息
     */
    @Override
    public void doAnalysis(JSONObject operation, JSONObject schemas, List<HTTPProtocolVO.ProtocolMapping> mappings) {
        // 获取接口定义中的parameters参数数组
        JSONArray parameters = operation.getJSONArray("parameters");
        // 空值判断，无参数时直接终止解析
        if (parameters == null || parameters.isEmpty()) {
            return;
        }

        // 遍历所有参数项，提取有效参数信息
        for (int i = 0; i < parameters.size(); i++) {
            JSONObject param = parameters.getJSONObject(i);
            // 跳过空参数对象
            if (param == null) {
                continue;
            }

            // 获取参数位置类型，仅处理query和path类型参数
            String in = param.getString("in");
            if (!"query".equals(in) && !"path".equals(in)) {
                continue;
            }

            // 提取参数基础属性信息
            String name = param.getString("name");
            boolean required = param.getBooleanValue("required");
            String description = param.getString("description");

            // 兼容双规范提取参数数据类型
            String type = extractType(param);

            // 构建参数映射对象，Parameters类型参数为平铺结构，无父级路径
            mappings.add(HTTPProtocolVO.ProtocolMapping
                    .builder()
                    .mappingType(getMappingType())
                    .parentPath(null)
                    .fieldName(name)
                    .mcpPath(name)
                    .mcpType(convertType(type))
                    .mcpDescription(description)
                    .isRequired(required ? 1 : 0)
                    .sortOrder(mappings.size() + 1)
                    .build());
        }
    }

    /**
     * 提取参数数据类型，兼容OpenAPI 3.0与Swagger 2.0规范
     * OpenAPI 3.0：类型存储在schema节点下
     * Swagger 2.0：类型直接存储在参数顶层节点
     *
     * @param param 单个参数的JSON节点对象
     * @return 提取的参数类型字符串，无匹配类型时返回默认string
     */
    private String extractType(JSONObject param) {
        // 适配OpenAPI 3.0规范，从schema节点获取类型
        JSONObject schema = param.getJSONObject("schema");
        if (schema != null && schema.containsKey("type")) {
            return schema.getString("type");
        }

        // 适配Swagger 2.0规范，从顶层节点获取类型
        if (param.containsKey("type")) {
            return param.getString("type");
        }

        // 兜底默认类型
        return "string";
    }
}