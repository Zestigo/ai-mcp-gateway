package com.c.domain.protocol.service.analysis.strategy;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 协议解析策略抽象基类
 * 定义协议解析的公共核心能力：DFS递归属性解析、路径拼接、循环引用防护、类型转换、驼峰命名转换
 * 所有具体解析策略类均继承此类，实现模板方法模式，统一解析流程
 *
 * @author cyh
 * @date 2026/03/29
 */
public abstract class AbstractProtocolAnalysisStrategy implements ProtocolAnalysisStrategy {

    /**
     * 获取当前解析场景对应的映射类型
     * 由子类实现，区分请求/响应两种解析场景
     *
     * @return 映射类型标识：request/response
     */
    protected abstract String getMappingType();

    /**
     * 深度优先递归解析属性核心方法
     * 遍历属性集合，分流处理引用、数组、基本类型、嵌套对象，构建完整的树状映射关系
     *
     * @param parentPath   父级属性路径，用于拼接完整字段路径
     * @param properties   当前层级属性集合JSON对象
     * @param requiredList 当前层级必填字段名称数组
     * @param definitions  全局模型定义库，用于解析$ref引用
     * @param mappings     映射结果集合，存储所有解析后的映射对象
     * @param visitedRefs  已处理引用集合，防止循环引用导致内存溢出
     */
    protected void parseProperties(String parentPath, JSONObject properties, JSONArray requiredList,
                                   JSONObject definitions, List<HTTPProtocolVO.ProtocolMapping> mappings,
                                   Set<String> visitedRefs) {
        // 基础空值校验，无属性则终止当前层级解析
        if (properties == null || properties.isEmpty()) {
            return;
        }

        String mappingType = getMappingType();

        // 遍历当前层级所有属性字段
        for (String propName : properties.keySet()) {
            JSONObject prop = properties.getJSONObject(propName);
            if (prop == null) {
                continue;
            }

            // 拼接字段完整路径：父路径.当前字段名
            String currentPath = (parentPath == null || parentPath.isEmpty()) ? propName : parentPath + "." + propName;

            // 提取字段基础信息
            String type = prop.getString("type");
            String description = prop.getString("description");

            // 处理$ref引用类型属性
            if (prop.containsKey("$ref")) {
                handleRefType(currentPath, prop, parentPath, propName, mappingType, requiredList, definitions,
                        mappings, visitedRefs);
                continue;
            }

            // 处理array数组类型属性
            if ("array".equalsIgnoreCase(type) && prop.containsKey("items")) {
                handleArrayType(currentPath, prop, parentPath, propName, mappingType, requiredList, definitions,
                        mappings, visitedRefs);
                continue;
            }

            // 处理普通基本数据类型属性，添加映射记录
            addMappingRecord(mappings, mappingType, parentPath, propName, currentPath, convertType(type), description
                    , requiredList);

            // 处理嵌套内联对象，递归解析子属性
            if (prop.containsKey("properties")) {
                parseProperties(currentPath, prop.getJSONObject("properties"), prop.getJSONArray("required"),
                        definitions, mappings, visitedRefs);
            }
        }
    }

    /**
     * 处理$ref引用类型属性解析逻辑
     * 解析引用指向的模型，添加对象节点映射，递归解析内部属性
     *
     * @param currentPath 当前字段完整路径
     * @param prop        当前属性JSON对象
     * @param parentPath  父级路径
     * @param fieldName   字段名称
     * @param mappingType 映射类型
     * @param required    必填字段列表
     * @param defs        全局模型库
     * @param mappings    映射结果集合
     * @param visitedRefs 已访问引用集合
     */
    private void handleRefType(String currentPath, JSONObject prop, String parentPath, String fieldName,
                               String mappingType, JSONArray required, JSONObject defs,
                               List<HTTPProtocolVO.ProtocolMapping> mappings, Set<String> visitedRefs) {
        // 解析引用名称，获取对应模型对象
        String ref = prop.getString("$ref");
        String refName = ref.substring(ref.lastIndexOf('/') + 1);
        JSONObject schema = defs != null ? defs.getJSONObject(refName) : null;

        if (schema == null) {
            return;
        }

        // 拼接字段描述信息
        String desc = prop.getString("description") != null ? prop.getString("description") : schema.getString(
                "description");
        // 添加引用对象节点映射记录
        addMappingRecord(mappings, mappingType, parentPath, fieldName, currentPath, "object", desc, required);

        // 未循环引用时，递归解析引用模型内部属性
        if (!visitedRefs.contains(ref)) {
            Set<String> nextVisited = new HashSet<>(visitedRefs);
            nextVisited.add(ref);
            parseProperties(currentPath, schema.getJSONObject("properties"), schema.getJSONArray("required"), defs,
                    mappings, nextVisited);
        }
    }

    /**
     * 处理array数组类型属性解析逻辑
     * 添加数组容器节点映射，递归解析数组元素内部属性
     *
     * @param currentPath 当前字段完整路径
     * @param prop        当前属性JSON对象
     * @param parentPath  父级路径
     * @param fieldName   字段名称
     * @param mappingType 映射类型
     * @param required    必填字段列表
     * @param defs        全局模型库
     * @param mappings    映射结果集合
     * @param visitedRefs 已访问引用集合
     */
    private void handleArrayType(String currentPath, JSONObject prop, String parentPath, String fieldName,
                                 String mappingType, JSONArray required, JSONObject defs,
                                 List<HTTPProtocolVO.ProtocolMapping> mappings, Set<String> visitedRefs) {
        // 添加数组容器节点映射记录
        addMappingRecord(mappings, mappingType, parentPath, fieldName, currentPath, "array", prop.getString(
                "description"), required);

        // 解析数组元素为引用类型的场景
        JSONObject items = prop.getJSONObject("items");
        if (items != null && items.containsKey("$ref")) {
            String ref = items.getString("$ref");
            String refName = ref.substring(ref.lastIndexOf('/') + 1);
            JSONObject itemSchema = defs != null ? defs.getJSONObject(refName) : null;

            if (itemSchema != null && !visitedRefs.contains(ref)) {
                Set<String> nextVisited = new HashSet<>(visitedRefs);
                nextVisited.add(ref);
                // 递归解析数组元素子属性，路径保持当前数组路径
                parseProperties(currentPath, itemSchema.getJSONObject("properties"), itemSchema.getJSONArray(
                        "required"), defs, mappings, nextVisited);
            }
        }
    }

    /**
     * 统一构建并添加属性映射记录
     * 封装ProtocolMapping对象，统一管理映射数据的创建逻辑
     *
     * @param mappings     映射结果集合
     * @param mappingType  映射类型
     * @param parentPath   父级路径
     * @param fieldName    字段名称
     * @param mcpPath      完整映射路径
     * @param mcpType      标准数据类型
     * @param desc         字段描述
     * @param requiredList 必填字段列表
     */
    protected void addMappingRecord(List<HTTPProtocolVO.ProtocolMapping> mappings, String mappingType,
                                    String parentPath, String fieldName, String mcpPath, String mcpType, String desc,
                                    JSONArray requiredList) {
        mappings.add(HTTPProtocolVO.ProtocolMapping
                .builder()
                .mappingType(mappingType)
                .parentPath(parentPath)
                .fieldName(fieldName)
                .mcpPath(mcpPath)
                .mcpType(mcpType)
                .mcpDesc(desc)
                .isRequired(requiredList != null && requiredList.contains(fieldName) ? 1 : 0)
                .sortOrder(mappings.size() + 1)
                .build());
    }

    /**
     * OpenAPI/Swagger数据类型转换为标准MCP协议类型
     * 统一归一化数据类型，屏蔽不同规范的类型差异
     *
     * @param type 原始数据类型字符串
     * @return 标准化后的MCP数据类型
     */
    protected String convertType(String type) {
        if (type == null) {
            return "string";
        }
        return switch (type.toLowerCase()) {
            case "string", "date", "datetime", "char" -> "string";
            case "integer", "long", "int", "number", "double", "float", "decimal" -> "number";
            case "boolean", "bool" -> "boolean";
            case "array", "list" -> "array";
            case "object", "map" -> "object";
            default -> "string";
        };
    }

    /**
     * 字符串转换为小驼峰格式
     * 用于生成根节点默认名称，适配Java命名规范
     *
     * @param name 原始命名字符串
     * @return 转换后的小驼峰命名字符串
     */
    protected String toLowerCamel(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return name.toLowerCase();
        }
        // 处理全大写缩写开头的特殊场景
        if (Character.isUpperCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1))) {
            return name.toLowerCase();
        }
        // 标准小驼峰转换
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}