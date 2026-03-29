package com.c.test.demo;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileCopyUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Swagger文档解析测试类
 * 用于解析Swagger OpenAPI格式的JSON文档，提取接口信息并构建HTTP协议配置对象
 * 核心功能：将Swagger接口定义转换为系统可识别的HTTP协议VO数据结构
 *
 * @author cyh
 * @date 2026/03/28
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class Swagger2McpProtocolHttpTest {

    /** 读取classpath下的Swagger API文档JSON资源文件 */
    @Value("classpath:swagger/api-docs-test03.json")
    private Resource apiDocs;

    /**
     * 测试主方法：解析Swagger文档并构建HTTP协议配置对象
     * 读取JSON资源文件，指定待解析的接口路径，执行解析逻辑并打印结果
     *
     * @throws Exception 读取文件、解析JSON过程中可能抛出的异常
     */
    @Test
    public void parseSwaggerAndBuildHTTPProtocolVO() throws Exception {
        // 将资源文件转换为UTF-8编码的字符串
        String json = new String(FileCopyUtils.copyToByteArray(apiDocs.getInputStream()), StandardCharsets.UTF_8);
        // 定义需要解析的接口路径列表，可灵活切换测试不同接口
        List<String> endpoints = List.of("/api/v1/mcp/get_company_employee");
//        List<String> endpoints = Arrays.asList("/api/v1/mcp/query-test03");
//        List<String> endpoints = Arrays.asList("/api/v1/mcp/query-test02");
//        List<String> endpoints = Arrays.asList("/api/v1/mcp/query-by-id-01");
//        List<String> endpoints = Arrays.asList("/api/v1/mcp/query-by-id-02");
//        List<String> endpoints = Arrays.asList("/api/v1/mcp/query-by-id-03");
        // 核心解析方法，生成HTTP协议配置集合
        List<HTTPProtocolVO> result = parse(json, endpoints);
        // 打印最终解析结果
        log.info("测试结果:{}", JSON.toJSONString(result));
    }

    /**
     * 核心解析方法：处理Swagger JSON，解析指定接口生成HTTP协议配置
     * 提取基础路径、接口信息、参数定义，封装为HTTPProtocolVO集合
     *
     * @param json      Swagger OpenAPI格式的JSON字符串
     * @param endpoints 需要解析的接口路径集合
     * @return 封装完成的HTTP协议配置对象列表
     */
    private List<HTTPProtocolVO> parse(String json, List<String> endpoints) {
        // 解析JSON字符串为根节点对象
        JSONObject root = JSON.parseObject(json);

        // 从servers节点提取接口基础URL
        JSONArray servers = root.getJSONArray("servers");
        JSONObject jsonObject = servers.getJSONObject(0);
        String baseUrl = jsonObject.getString("url");

        // 获取所有接口路径定义、数据模型定义
        JSONObject paths = root.getJSONObject("paths");
        JSONObject schemas = root
                .getJSONObject("components")
                .getJSONObject("schemas");

        // 存储最终解析结果
        List<HTTPProtocolVO> list = new ArrayList<>();

        // 遍历所有需要解析的接口路径
        for (String endpoint : endpoints) {
            // 获取当前接口的详细配置
            JSONObject pathItem = paths.getJSONObject(endpoint);
            // 接口不存在则跳过
            if (pathItem == null) continue;

            // 检测接口请求方法（get/post/put/delete）
            String method = detectMethod(pathItem);
            // 获取接口操作详情
            JSONObject operation = pathItem.getJSONObject(method);

            // 初始化HTTP协议配置对象
            HTTPProtocolVO vo = new HTTPProtocolVO();
            // 拼接完整请求URL
            vo.setHttpUrl(baseUrl + endpoint);
            // 设置请求方法
            vo.setHttpMethod(method);
            // 设置默认请求头
            vo.setHttpHeaders(JSON.toJSONString(new HashMap<>() {{
                put("Content-Type", "application/json");
            }}));
            // 设置默认超时时间30秒
            vo.setTimeout(30000);

            // 存储参数映射关系
            List<HTTPProtocolVO.ProtocolMapping> mappings = new ArrayList<>();

            // 1. 解析请求体参数（对象入参）
            JSONObject requestBody = operation.getJSONObject("requestBody");
            if (requestBody != null) {
                JSONObject content = requestBody.getJSONObject("content");
                JSONObject appJson = content.getJSONObject("application/json");
                if (appJson != null) {
                    JSONObject schema = appJson.getJSONObject("schema");
                    String ref = schema.getString("$ref");

                    if (ref != null) {
                        // 截取引用的模型名称
                        String refName = ref.substring(ref.lastIndexOf('/') + 1);
                        // 获取模型定义
                        JSONObject reqSchema = schemas.getJSONObject(refName);
                        // 转换为小驼峰命名作为根字段名
                        String rootName = toLowerCamel(refName);

                        // 构建根参数映射
                        HTTPProtocolVO.ProtocolMapping rootMapping = HTTPProtocolVO.ProtocolMapping
                                .builder()
                                .mappingType("request")
                                .parentPath(null)
                                .fieldName(rootName)
                                .mcpPath(rootName)
                                .mcpType(convertType(reqSchema.getString("type")))
                                .mcpDesc(reqSchema.getString("description"))
                                .isRequired(1)
                                .sortOrder(1)
                                .build();
                        mappings.add(rootMapping);

                        // 递归解析模型的所有属性
                        parseProperties(rootName, reqSchema.getJSONObject("properties"), reqSchema.getJSONArray(
                                "required"), schemas, mappings);
                    }
                }
            }

            // 2. 解析普通参数（query/path入参）
            JSONArray parameters = operation.getJSONArray("parameters");
            if (parameters != null) {
                for (int i = 0; i < parameters.size(); i++) {
                    JSONObject param = parameters.getJSONObject(i);
                    String in = param.getString("in");
                    // 仅处理query和path类型参数
                    if (!"query".equals(in) && !"path".equals(in)) continue;

                    // 提取参数基础信息
                    String name = param.getString("name");
                    boolean required = param.getBooleanValue("required");
                    String description = param.getString("description");

                    // 提取参数类型定义
                    JSONObject schema = param.getJSONObject("schema");
                    String type = schema.getString("type");
                    String ref = schema.getString("$ref");

                    if (ref != null) {
                        // 参数引用了数据模型，递归解析模型属性
                        String refName = ref.substring(ref.lastIndexOf('/') + 1);
                        JSONObject reqSchema = schemas.getJSONObject(refName);

                        // 补充类型和描述信息
                        if (type == null) type = reqSchema.getString("type");
                        if (description == null) description = reqSchema.getString("description");

                        // 构建参数映射
                        HTTPProtocolVO.ProtocolMapping rootMapping = HTTPProtocolVO.ProtocolMapping
                                .builder()
                                .mappingType("request")
                                .parentPath(null)
                                .fieldName(name)
                                .mcpPath(name)
                                .mcpType(convertType(type))
                                .mcpDesc(description)
                                .isRequired(required ? 1 : 0)
                                .sortOrder(mappings.size() + 1)
                                .build();
                        mappings.add(rootMapping);

                        // 递归解析模型属性
                        parseProperties(name, reqSchema.getJSONObject("properties"), reqSchema.getJSONArray("required"
                        ), schemas, mappings);
                    } else {
                        // 普通基础类型参数，直接构建映射
                        HTTPProtocolVO.ProtocolMapping mapping = HTTPProtocolVO.ProtocolMapping
                                .builder()
                                .mappingType("request")
                                .parentPath(null)
                                .fieldName(name)
                                .mcpPath(name)
                                .mcpType(convertType(type))
                                .mcpDesc(description)
                                .isRequired(required ? 1 : 0)
                                .sortOrder(mappings.size() + 1)
                                .build();
                        mappings.add(mapping);
                    }
                }
            }

            // 设置参数映射关系
            vo.setMappings(mappings);
            // 添加到结果集合
            list.add(vo);
        }
        return list;
    }

    /**
     * 递归解析数据模型属性
     * 处理嵌套对象，生成完整的参数路径和映射关系
     *
     * @param parentMcpPath 父级参数路径，用于拼接嵌套路径
     * @param properties    数据模型的属性集合
     * @param requiredList  必填参数字段集合
     * @param definitions   全局数据模型定义
     * @param mappings      存储参数映射关系的集合
     */
    private void parseProperties(String parentMcpPath, JSONObject properties, JSONArray requiredList,
                                 JSONObject definitions, List<HTTPProtocolVO.ProtocolMapping> mappings) {
        // 无属性则直接返回
        if (properties == null) return;

        int sortOrder = 1;
        // 遍历模型所有属性
        for (String propName : properties.keySet()) {
            JSONObject prop = properties.getJSONObject(propName);

            // 拼接嵌套参数路径，格式：父路径.当前属性名
            String currentMcpPath = parentMcpPath + "." + propName;

            // 定义有效Schema对象，优先处理引用类型
            JSONObject effectiveSchema = prop;
            String type = prop.getString("type");
            String description = prop.getString("description");

            // 属性引用了其他数据模型，替换为实际模型定义
            if (prop.containsKey("$ref")) {
                String ref = prop.getString("$ref");
                String refName = ref.substring(ref.lastIndexOf('/') + 1);
                effectiveSchema = definitions.getJSONObject(refName);
                // 补充缺失的类型和描述
                if (type == null) type = effectiveSchema.getString("type");
                if (description == null) description = effectiveSchema.getString("description");
            }

            // 构建当前属性的映射关系
            HTTPProtocolVO.ProtocolMapping mapping = HTTPProtocolVO.ProtocolMapping
                    .builder()
                    .mappingType("request")
                    .parentPath(parentMcpPath)
                    .fieldName(propName)
                    .mcpPath(currentMcpPath)
                    .mcpType(convertType(type))
                    .mcpDesc(description)
                    .isRequired(requiredList != null && requiredList.contains(propName) ? 1 : 0)
                    .sortOrder(sortOrder++)
                    .build();
            mappings.add(mapping);

            // 存在子属性，递归继续解析
            if (effectiveSchema.containsKey("properties")) {
                parseProperties(currentMcpPath, effectiveSchema.getJSONObject("properties"),
                        effectiveSchema.getJSONArray("required"), definitions, mappings);
            }
        }
    }

    /**
     * Swagger类型转换为MCP标准数据类型
     * MCP支持类型：string/number/boolean/object/array
     *
     * @param type Swagger定义的参数类型
     * @return 转换后的MCP标准类型
     */
    private String convertType(String type) {
        if (type == null) return "string";
        return switch (type.toLowerCase()) {
            case "string", "char", "date", "datetime" -> "string";
            case "integer", "int", "long", "double", "float", "number" -> "number";
            case "boolean", "bool" -> "boolean";
            case "array", "list" -> "array";
            default -> "object";
        };
    }

    /**
     * 检测接口的HTTP请求方法
     * 优先识别post，其次get/put/delete，默认返回post
     *
     * @param pathItem 接口路径配置对象
     * @return 检测到的请求方法（小写）
     */
    private String detectMethod(JSONObject pathItem) {
        if (pathItem.containsKey("post")) return "post";
        if (pathItem.containsKey("get")) return "get";
        if (pathItem.containsKey("put")) return "put";
        if (pathItem.containsKey("delete")) return "delete";
        return "post";
    }

    /**
     * 字符串首字母转换为小写（小驼峰命名）
     *
     * @param name 原始命名字符串
     * @return 首字母小写后的字符串
     */
    private String toLowerCamel(String name) {
        if (name == null || name.isEmpty()) return name;
        char[] cs = name.toCharArray();
        cs[0] = Character.toLowerCase(cs[0]);
        return new String(cs);
    }

}