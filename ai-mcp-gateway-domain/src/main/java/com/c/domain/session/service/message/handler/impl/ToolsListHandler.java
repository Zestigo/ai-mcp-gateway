package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.adapter.repository.GatewayRepository;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.gateway.McpGatewayConfigVO;
import com.c.domain.session.model.valobj.gateway.McpGatewayToolConfigVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import com.c.types.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP 协议 tools/list 请求处理器实现类
 * 核心职责：接收 MCP 工具列表查询请求，基于网关配置数据动态构建符合 MCP 协议规范的工具集合
 * 最终封装为 JSON-RPC 标准响应返回，支持嵌套结构的工具入参 Schema 生成
 *
 * @author cyh
 * @date 2026/03/25
 */
@Slf4j
@Service("toolsListHandler")
@RequiredArgsConstructor
public class ToolsListHandler implements IRequestHandler {

    /** 网关配置仓储层，用于查询网关基础配置与工具字段配置 */
    private final GatewayRepository gatewayRepository;

    /**
     * 处理 MCP 工具列表查询请求入口方法
     * 校验请求消息类型，异步执行工具列表构建逻辑，统一异常处理
     *
     * @param gatewayId 网关唯一标识，用于定位对应网关配置
     * @param message   MCP 协议请求消息体
     * @return 封装完成的 JSON-RPC 响应流
     */
    @Override
    public Flux<McpSchemaVO.JSONRPCResponse> handle(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        // 校验请求消息类型：仅支持 JSON-RPC 请求消息，拒绝非请求类型消息
        if (!(message instanceof McpSchemaVO.JSONRPCRequest req)) {
            return Flux.error(new AppException("MCP-400", "Only request messages are supported"));
        }

        // 异步执行阻塞操作，避免阻塞反应式流，使用弹性线程池隔离阻塞任务
        return Mono
                .fromCallable(() -> executeBuild(gatewayId, req))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::just)
                // 全局异常处理：记录错误日志，封装统一的业务异常返回
                .onErrorResume(e -> {
                    log.error("Failed to build tools list for gateway: {}", gatewayId, e);
                    return Flux.error(e instanceof AppException ? e : new AppException("MCP-500",
                            "Internal Server " + "Error"));
                });
    }

    /**
     * 执行工具列表构建核心逻辑
     * 查询网关配置与工具字段配置，调用构建方法生成标准 MCP 工具列表，封装成功响应
     *
     * @param gatewayId 网关唯一标识
     * @param req       JSON-RPC 请求对象，包含请求ID等核心参数
     * @return 封装工具列表数据的 JSON-RPC 成功响应
     */
    private McpSchemaVO.JSONRPCResponse executeBuild(String gatewayId, McpSchemaVO.JSONRPCRequest req) {
        // 查询网关基础配置，配置不存在则抛出 404 异常
        McpGatewayConfigVO gatewayConfig = Optional
                .ofNullable(gatewayRepository.queryMcpGatewayConfigByGatewayId(gatewayId))
                .orElseThrow(() -> new AppException("MCP-404", "Gateway config not found: " + gatewayId));

        // 查询当前网关下所有工具字段映射配置
        List<McpGatewayToolConfigVO> allConfigs = gatewayRepository.queryMcpGatewayToolConfigListByGatewayId(gatewayId);

        // 基于网关配置和字段配置，构建 MCP 协议标准工具列表
        List<McpSchemaVO.Tool> tools = buildMcpTools(gatewayConfig, allConfigs);
        // 封装成功响应，携带请求ID和工具列表数据
        return McpSchemaVO.JSONRPCResponse.ofSuccess(req.id(), Map.of("tools", tools));
    }

    /**
     * 构建 MCP 协议标准工具集合
     * 对工具配置按工具ID分组，递归构建嵌套字段结构，生成符合规范的 JSON Schema
     *
     * @param gatewayConfig 网关基础配置，包含工具名称、描述等信息
     * @param allConfigs    工具字段映射配置列表，包含字段类型、层级、是否必填等配置
     * @return 符合 MCP 协议规范的标准工具列表
     */
    private List<McpSchemaVO.Tool> buildMcpTools(McpGatewayConfigVO gatewayConfig,
                                                 List<McpGatewayToolConfigVO> allConfigs) {
        // 工具配置为空时，直接返回空列表
        if (allConfigs == null || allConfigs.isEmpty()) {
            return Collections.emptyList();
        }

        // 按工具ID对字段配置分组，实现一个工具对应多个字段的映射关系
        Map<Long, List<McpGatewayToolConfigVO>> toolGroup = allConfigs
                .stream()
                .collect(Collectors.groupingBy(McpGatewayToolConfigVO::getToolId));

        // 遍历分组后的工具配置，逐个构建 MCP 工具对象
        return toolGroup
                .values()
                .stream()
                .map(configs -> {

                    // 构建父子字段映射关系：父路径 -> 子字段配置列表，用于递归构建嵌套结构
                    Map<String, List<McpGatewayToolConfigVO>> childrenMap = configs
                            .stream()
                            .filter(c -> c.getParentPath() != null)
                            .collect(Collectors.groupingBy(McpGatewayToolConfigVO::getParentPath));

                    // 筛选根字段（无父路径），并按照排序字段升序排列
                    List<McpGatewayToolConfigVO> roots = configs
                            .stream()
                            .filter(c -> c.getParentPath() == null || c
                                    .getParentPath()
                                    .isEmpty())
                            .sorted(Comparator.comparingInt(o -> o.getSortOrder() != null ? o.getSortOrder() : 0))
                            .collect(Collectors.toList());

                    // 递归构建嵌套字段结构，生成 Schema 节点数据
                    SchemaNode rootNode = buildNodeStructure(roots, childrenMap);

                    // 基于节点数据构建 JSON Schema 对象，符合 MCP 协议入参规范
                    McpSchemaVO.JsonSchema inputSchema = new McpSchemaVO.JsonSchema("object", rootNode.properties(),
                            rootNode
                                    .required()
                                    .isEmpty() ? null : rootNode.required(), false, null, null);

                    // 构建 MCP 标准工具对象，包含名称、描述、入参 Schema
                    return new McpSchemaVO.Tool(gatewayConfig.getToolName(), gatewayConfig.getToolDescription(),
                            inputSchema);
                })
                .collect(Collectors.toList());
    }

    /**
     * 递归构建嵌套字段结构节点
     * 遍历当前层级字段，处理子字段递归，封装属性集合与必填字段列表
     *
     * @param nodes       当前层级的字段配置列表
     * @param childrenMap 父子字段映射关系
     * @return 封装完成的字段结构节点，包含属性和必填字段
     */
    private SchemaNode buildNodeStructure(List<McpGatewayToolConfigVO> nodes, Map<String,
            List<McpGatewayToolConfigVO>> childrenMap) {
        // 有序存储字段属性，保证 Schema 结构顺序与配置一致
        Map<String, Object> properties = new LinkedHashMap<>();
        // 存储必填字段名称列表
        List<String> required = new ArrayList<>();

        // 遍历当前层级所有字段配置，逐个构建字段属性
        for (McpGatewayToolConfigVO node : nodes) {
            Map<String, Object> propertyMap = new LinkedHashMap<>();
            // 设置字段数据类型
            propertyMap.put("type", node.getMcpType());

            // 字段描述不为空时，添加描述信息
            if (node.getMcpDescription() != null) {
                propertyMap.put("description", node.getMcpDescription());
            }

            // 递归处理子字段：当前字段存在子字段时，构建子层级结构
            List<McpGatewayToolConfigVO> children = childrenMap.get(node.getMcpPath());
            if (children != null && !children.isEmpty()) {
                // 子字段按照排序规则排序
                children.sort(Comparator.comparingInt(o -> o.getSortOrder() != null ? o.getSortOrder() : 0));
                // 递归构建子字段结构
                SchemaNode childResult = buildNodeStructure(children, childrenMap);
                // 设置子字段属性
                propertyMap.put("properties", childResult.properties());
                // 子字段存在必填项时，添加必填字段列表
                if (!childResult
                        .required()
                        .isEmpty()) {
                    propertyMap.put("required", childResult.required());
                }
            }

            // 将当前字段属性加入集合
            properties.put(node.getFieldName(), propertyMap);
            // 配置为必填字段时，加入必填列表
            if (Integer
                    .valueOf(1)
                    .equals(node.getIsRequired())) {
                required.add(node.getFieldName());
            }
        }

        // 返回封装好的结构节点
        return new SchemaNode(properties, required);
    }

    /**
     * 字段结构封装对象
     * 统一管理 JSON Schema 中的字段属性集合和必填字段列表
     *
     * @param properties 字段属性集合（字段名 -> 字段配置）
     * @param required   必填字段名称列表
     */
    private record SchemaNode(Map<String, Object> properties, List<String> required) {
    }
}