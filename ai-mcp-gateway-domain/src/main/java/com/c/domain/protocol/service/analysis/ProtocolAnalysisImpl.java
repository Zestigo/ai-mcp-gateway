package com.c.domain.protocol.service.analysis;

import com.alibaba.fastjson2.JSONObject;
import com.c.domain.protocol.model.entity.AnalysisCommandEntity;
import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;
import com.c.domain.protocol.service.ProtocolAnalysis;
import com.c.domain.protocol.service.analysis.factory.ProtocolVOFactory;
import com.c.domain.protocol.service.analysis.parser.OpenApiParser;
import com.c.domain.protocol.service.analysis.strategy.ProtocolAnalysisStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 协议解析服务实现类
 * 协调解析器与策略链，完成完整的协议解析流程
 *
 * @author cyh
 * @date 2026/03/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProtocolAnalysisImpl implements ProtocolAnalysis {

    /** OpenAPI原始数据解析器 */
    private final OpenApiParser parser;
    /** 协议解析策略集合（请求体/参数/响应体） */
    private final List<ProtocolAnalysisStrategy> strategies;

    /** 标准HTTP请求方法集合 */
    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "delete", "patch");

    /**
     * 初始化方法，打印加载的解析策略
     */
    @PostConstruct
    public void init() {
        // 启动时打印策略加载顺序，便于排查执行顺序问题
        log.info("协议解析策略加载完成，顺序为: {}", strategies
                .stream()
                .map(s -> s
                        .getClass()
                        .getSimpleName())
                .collect(Collectors.toList()));
    }

    /**
     * 执行协议解析主流程
     *
     * @param commandEntity 解析命令实体
     * @return 解析后的HTTPProtocolVO列表
     */
    @Override
    public List<HTTPProtocolVO> doAnalysis(AnalysisCommandEntity commandEntity) {
        // 入参合法性校验
        if (null == commandEntity || !commandEntity.isValid()) {
            log.warn("协议解析命令无效，请检查 openApiJson 是否缺失");
            return Collections.emptyList();
        }

        List<HTTPProtocolVO> result = new ArrayList<>();
        try {
            // 1. 解析原始OpenAPI JSON字符串
            JSONObject root = parser.parse(commandEntity.getOpenApiJson());
            if (null == root) return Collections.emptyList();

            // 2. 提取服务基础地址
            String baseUrl = extractBaseUrl(root);
            // 3. 获取所有接口路径定义
            JSONObject paths = root.getJSONObject("paths");
            if (null == paths) return Collections.emptyList();

            // 4. 提取全局模型定义（兼容OpenAPI3/Swagger2）
            JSONObject schemas = extractDefinitions(root);

            // 5. 遍历需要解析的接口地址
            for (String endpoint : commandEntity.getEndpoints()) {
                JSONObject pathItem = paths.getJSONObject(endpoint);
                if (null == pathItem) {
                    log.warn("未在 Swagger 文档中找到指定路径: {}", endpoint);
                    continue;
                }

                // 6. 遍历当前路径下的所有请求方法（get/post/put等）
                for (String methodKey : pathItem.keySet()) {
                    String method = methodKey.toLowerCase();
                    // 只处理标准HTTP方法
                    if (!HTTP_METHODS.contains(method)) continue;

                    // 获取接口操作节点信息
                    JSONObject operation = pathItem.getJSONObject(methodKey);
                    if (null == operation) continue;

                    // 7. 创建基础协议VO对象
                    HTTPProtocolVO vo = ProtocolVOFactory.create(baseUrl, endpoint, method);
                    List<HTTPProtocolVO.ProtocolMapping> mappings = new ArrayList<>();

                    // 8. 按顺序执行所有解析策略
                    for (ProtocolAnalysisStrategy strategy : strategies) {
                        try {
                            // 策略匹配则执行解析
                            if (strategy.match(operation)) {
                                strategy.doAnalysis(operation, schemas, mappings);
                            }
                        } catch (Exception strategyEx) {
                            // 单个策略异常不影响整体流程
                            log.error("策略执行异常 strategy: {} path: {}", strategy
                                    .getClass()
                                    .getSimpleName(), endpoint, strategyEx);
                        }
                    }

                    // 9. 有解析结果则加入返回列表
                    if (!mappings.isEmpty()) {
                        vo.setMappings(mappings);
                        result.add(vo);
                    }
                }
            }
        } catch (Exception e) {
            log.error("协议解析流程崩溃: {}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * 提取全局模型定义库，兼容OpenAPI3.0与Swagger2.0
     *
     * @param root 根节点对象
     * @return 模型定义JSONObject
     */
    private JSONObject extractDefinitions(JSONObject root) {
        // OpenAPI3.0：components.schemas
        JSONObject components = root.getJSONObject("components");
        if (components != null && components.containsKey("schemas")) {
            return components.getJSONObject("schemas");
        }
        // Swagger2.0：definitions
        if (root.containsKey("definitions")) {
            return root.getJSONObject("definitions");
        }
        // 无模型定义返回空对象
        return new JSONObject();
    }

    /**
     * 提取接口基础地址，兼容多版本OpenAPI
     *
     * @param root 根节点对象
     * @return 基础地址字符串
     */
    private String extractBaseUrl(JSONObject root) {
        // OpenAPI3.0 使用 servers 节点
        if (root.containsKey("servers")) {
            return Optional
                    .ofNullable(root.getJSONArray("servers"))
                    .filter(s -> !s.isEmpty())
                    .map(s -> s
                            .getJSONObject(0)
                            .getString("url"))
                    .orElse("");
        }
        // Swagger2.0 使用 host + basePath
        String host = root.getString("host");
        String basePath = root.getString("basePath");
        if (host != null) {
            return "http://" + host + (basePath != null ? basePath : "");
        }
        return "";
    }
}