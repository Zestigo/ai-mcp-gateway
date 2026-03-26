package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.adapter.repository.GatewayRepository;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.gateway.McpToolConfigVO;
import com.c.domain.session.model.valobj.gateway.McpToolProtocolConfigVO;
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
 * MCP工具列表查询处理器
 * 处理tools/list协议请求，将工具配置转换为标准JSON Schema结构
 * 为AI客户端提供标准化的工具能力发现服务
 *
 * @author cyh
 * @date 2026/03/26
 */
@Slf4j
@Service("toolsListHandler")
@RequiredArgsConstructor
public class ToolsListHandler implements IRequestHandler {

    /** 网关配置仓储 */
    private final GatewayRepository gatewayRepository;

    /**
     * 处理MCP工具列表查询请求
     *
     * @param gatewayId 网关唯一标识
     * @param message   JSON-RPC请求消息
     * @return 工具列表响应流
     */
    @Override
    public Flux<McpSchemaVO.JSONRPCResponse> handle(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        // 校验消息类型是否为合法请求
        if (!(message instanceof McpSchemaVO.JSONRPCRequest req)) {
            return Flux.error(new AppException("MCP-400", "Only JSON-RPC Request is supported"));
        }

        return Mono.fromCallable(() -> {
                       // 查询网关下所有工具配置信息
                       List<McpToolConfigVO> toolConfigs =
                               gatewayRepository.queryMcpGatewayToolConfigListByGatewayId(gatewayId);

                       // 无工具数据时返回空列表
                       if (toolConfigs == null || toolConfigs.isEmpty()) {
                           log.warn("MCP_TOOLS_EMPTY | gatewayId={}", gatewayId);
                           return McpSchemaVO.JSONRPCResponse.ofSuccess(req.id(), Map.of("tools",
                                   Collections.emptyList()));
                       }

                       // 将业务配置转换为MCP协议标准工具对象
                       List<McpSchemaVO.Tool> mcpTools = toolConfigs
                               .stream()
                               .map(config -> {
                                   // 获取工具字段映射规则
                                   List<McpToolProtocolConfigVO.ProtocolMapping> mappings = config
                                           .getMcpToolProtocolConfigVO()
                                           .getRequestProtocolMappings();
                                   // 递归构建JSON Schema结构
                                   McpSchemaVO.JsonSchema inputSchema = buildToolSchema(mappings);
                                   return new McpSchemaVO.Tool(config.getToolName(), config.getToolDescription(),
                                           inputSchema);
                               })
                               .toList();

                       log.info("MCP_TOOLS_LIST_LOADED | gatewayId={} | count={}", gatewayId, mcpTools.size());
                       return McpSchemaVO.JSONRPCResponse.ofSuccess(req.id(), Map.of("tools", mcpTools));
                   })
                   // 数据库阻塞操作切换至弹性线程池
                   .subscribeOn(Schedulers.boundedElastic())
                   // 统一异常处理
                   .onErrorResume(e -> {
                       log.error("MCP_TOOLS_LIST_ERROR | gatewayId={}", gatewayId, e);
                       return Mono.just(McpSchemaVO.JSONRPCResponse.ofError(req.id(), -32603, e.getMessage(), null));
                   })
                   .flux();
    }

    /**
     * 构建工具JSON Schema结构
     *
     * @param mappings 协议字段映射列表
     * @return 标准JSON Schema对象
     */
    private McpSchemaVO.JsonSchema buildToolSchema(List<McpToolProtocolConfigVO.ProtocolMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return new McpSchemaVO.JsonSchema("object", new HashMap<>(), null, false, null, null);
        }

        // 构建父子节点映射关系
        Map<String, List<McpToolProtocolConfigVO.ProtocolMapping>> treeMap = mappings
                .stream()
                .filter(m -> m.getParentPath() != null && !m
                        .getParentPath()
                        .isEmpty())
                .collect(Collectors.groupingBy(McpToolProtocolConfigVO.ProtocolMapping::getParentPath));

        // 获取根节点字段：筛选出没有父路径的字段作为多叉树的根
        List<McpToolProtocolConfigVO.ProtocolMapping> roots = mappings
                .stream()
                .filter(m -> m.getParentPath() == null || m
                        .getParentPath()
                        .isEmpty())
                // 2. 物理排序：保证生成的 JSON 字段顺序与数据库配置一致，方便 AI 顺序理解
                .sorted(Comparator.comparingInt(m -> m.getSortOrder() != null ? m.getSortOrder() : 0))
                .toList();

        // 递归构建Schema节点
        SchemaNode result = recursiveBuild(roots, treeMap);

        // 组装MCP标准JSON Schema根对象
        return new McpSchemaVO.JsonSchema("object",             // JSON Schema 规范：MCP 工具入参根节点必须为对象类型
                result.properties(),             // 递归生成的参数树：由 mcp_protocol_mapping 表层级转换而来
                result
                        .required()
                        .isEmpty() ? null : result.required(),         // 约束：显式声明必填字段数组，若无必填项则设为 null 以简化报文
                false,                           // 安全策略：禁止 AI 传入未定义的额外参数 (additionalProperties=false)
                null,                            // 扩展位：$defs (Draft 2019-09+) 预留，当前采用递归平铺模式
                null                             // 兼容位：definitions (旧版规范) 预留，确保协议向前兼容性
        );
    }

    /**
     * 递归构建多叉树结构的 JSON Schema
     *
     * @param currentLevelNodes 当前层级的节点列表（同属于同一个父路径）
     * @param treeMap           全局节点索引表（Key: 父路径, Value: 该路径下的所有子节点）
     * @return 封装了当前层级 properties 和 required 的 SchemaNode 单元
     */
    private SchemaNode recursiveBuild(List<McpToolProtocolConfigVO.ProtocolMapping> currentLevelNodes, Map<String,
            List<McpToolProtocolConfigVO.ProtocolMapping>> treeMap) {
        // 1. 结构初始化：使用 LinkedHashMap 维护字段插入顺序，满足 AI 对参数阅读的逻辑一致性
        Map<String, Object> properties = new LinkedHashMap<>(currentLevelNodes.size());
        List<String> requiredFields = new ArrayList<>();

        for (McpToolProtocolConfigVO.ProtocolMapping node : currentLevelNodes) {
            // 2. 节点元数据容器：存储 type, description, properties 等符合 JSON Schema 规范的键值对
            Map<String, Object> fieldMeta = new LinkedHashMap<>();

            // 核心：映射数据类型。若数据库未配置，默认降级为 string 以保证协议自洽
            fieldMeta.put("type", Optional
                    .ofNullable(node.getMcpType())
                    .orElse("string"));

            // 指令：注入字段描述信息。这是 AI 能够正确提取参数的“灵魂”，非空则注入
            if (node.getMcpDescription() != null && !node
                    .getMcpDescription()
                    .isBlank()) {
                fieldMeta.put("description", node.getMcpDescription());
            }

            // 3. 深度遍历 (Deep Dive)：通过当前节点的 mcpPath 在索引表中查找下属子节点
            List<McpToolProtocolConfigVO.ProtocolMapping> children = treeMap.get(node.getMcpPath());
            if (children != null && !children.isEmpty()) {
                // 物理排序：确保嵌套对象内部的字段也遵循配置定义的 sort_order 顺序
                children.sort(Comparator.comparingInt(m -> m.getSortOrder() != null ? m.getSortOrder() : 0));

                // 进入下一层级递归
                SchemaNode childNode = recursiveBuild(children, treeMap);

                // 组装嵌套结构：将子层的构建结果回填至父层的 properties 中
                if (!childNode
                        .properties()
                        .isEmpty()) {
                    fieldMeta.put("properties", childNode.properties());
                }
                // 约束冒泡：将子层定义的 required 数组按照规范声明在当前对象级
                if (!childNode
                        .required()
                        .isEmpty()) {
                    fieldMeta.put("required", childNode.required());
                }
            }

            // 4. 节点锚定：以 fieldName 为键。注意：fieldName 必须与 AI 看到的协议名称严格对齐
            properties.put(node.getFieldName(), fieldMeta);

            // 5. 状态转换：将业务层面的 Integer(1) 状态转换为协议层面的 required 约束列表
            if (node.getIsRequired() != null && node.getIsRequired() == 1) {
                requiredFields.add(node.getFieldName());
            }
        }

        // 向上层回溯当前组装完毕的节点单元
        return new SchemaNode(properties, requiredFields);
    }

    /**
     * Schema 结构节点中间承载对象
     * 在多叉树递归构建过程中，用于临时封装当前层级的属性集合与约束规则。
     * properties 属性映射表：存储当前层级下所有字段的元数据（Key 为 fieldName，Value 为字段配置 Map）
     * required   必填约束列表：存储当前层级中所有 isRequired=1 的字段名，符合 JSON Schema 规范
     */
    private record SchemaNode(Map<String, Object> properties, List<String> required) {
    }
}