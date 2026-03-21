package com.c.domain.session.model.valobj;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * MCP 协议 Schema 数据值对象
 * 核心能力：基于Java 21密封接口+Record实现JSON-RPC 2.0协议高性能、类型安全解析
 *
 * @author cyh
 * @date 2026/03/20
 */
@Slf4j
public final class McpSchemaVO {

    /**
     * 全局复用的ObjectMapper实例（线程安全）
     * 配置说明：1.忽略未知属性 2.序列化仅包含非空字段
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_ABSENT);

    /**
     * 反序列化JSON-RPC 2.0消息
     * 性能优化：先解析为JsonNode探测消息类型，避免双倍转换开销
     *
     * @param jsonText JSON-RPC 2.0格式字符串
     * @return 类型安全的JSONRPCMessage实例（请求/响应）
     * @throws IOException              JSON解析失败时抛出
     * @throws IllegalArgumentException 消息结构不符合JSON-RPC 2.0规范时抛出
     */
    public static JSONRPCMessage deserializeJsonRpcMessage(String jsonText) throws IOException {
        log.debug("Parsing JSON-RPC message: {}", jsonText);
        // 解析为轻量级树结构，探测消息类型
        JsonNode root = MAPPER.readTree(jsonText);
        // 请求消息：包含method字段（JSON-RPC 2.0标准）
        if (root.has("method")) {
            return MAPPER.treeToValue(root, JSONRPCRequest.class);
        }
        // 响应消息：包含result或error字段（JSON-RPC 2.0标准）
        if (root.has("result") || root.has("error")) {
            return MAPPER.treeToValue(root, JSONRPCResponse.class);
        }
        throw new IllegalArgumentException("Unrecognized JSON-RPC message structure: " + jsonText);
    }

    /**
     * JSON-RPC 2.0消息密封接口
     * 设计约束：仅允许当前类内的Record实现，保证类型安全
     */
    public sealed interface JSONRPCMessage permits JSONRPCRequest, JSONRPCResponse {
        /**
         * JSON-RPC协议版本号（固定为"2.0"）
         *
         * @return 协议版本字符串
         */
        @JsonProperty("jsonrpc")
        String jsonrpc();
    }

    /**
     * JSON-RPC 2.0请求消息实体
     * 不可变记录，包含协议核心字段：版本、方法、ID、参数
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JSONRPCRequest(String jsonrpc, String method, Object id, Object params) implements JSONRPCMessage {
    }

    /**
     * JSON-RPC 2.0响应消息实体
     * 不可变记录，包含协议核心字段：版本、ID、结果、错误信息
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JSONRPCResponse(String jsonrpc, Object id, Object result,
                                  JSONRPCError error) implements JSONRPCMessage {

        /**
         * JSON-RPC 2.0错误详情实体
         * 包含错误码、错误信息、附加数据
         */
        public record JSONRPCError(int code, String message, Object data) {
        }
    }
}