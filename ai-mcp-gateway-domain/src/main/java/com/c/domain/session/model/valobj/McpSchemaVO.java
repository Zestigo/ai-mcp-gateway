package com.c.domain.session.model.valobj;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

/**
 * MCP 协议 JSON-RPC 消息模型与工具类
 * 统一封装：消息序列化、反序列化、请求/响应结构、MCP 协议定义
 * 纯值对象 + 工具类，无状态
 *
 * @author cyh
 * @date 2026/03/25
 */
@Slf4j
public final class McpSchemaVO {

    /** MCP 最新协议版本 */
    public static final String LATEST_PROTOCOL_VERSION = "2024-11-05";

    /** JSON-RPC 固定版本 */
    public static final String JSONRPC_VERSION = "2.0";

    /** 私有构造：禁止实例化 */
    private McpSchemaVO() {
    }

    // =========================
    // JSON-RPC 方法名 & 错误码
    // =========================

    /** MCP 支持的 JSON-RPC 方法常量 */
    public static final class Methods {
        /** 初始化握手 */
        public static final String INITIALIZE = "initialize";
        /** 获取工具列表 */
        public static final String LIST_TOOLS = "tools/list";
        /** 调用工具 */
        public static final String CALL_TOOL = "tools/call";
    }

    /** JSON-RPC 标准错误码 */
    public static final class ErrorCodes {
        public static final int PARSE_ERROR = -32700;
        public static final int INVALID_REQUEST = -32600;
        public static final int METHOD_NOT_FOUND = -32601;
        public static final int INVALID_PARAMS = -32602;
        public static final int INTERNAL_ERROR = -32603;
    }

    // =========================
    // JSON 序列化配置
    // =========================

    /** 全局单例 ObjectMapper：宽松解析、忽略未知字段、不序列化 null */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
            .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /** Map 类型引用，避免泛型擦除 */
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    // =========================
    // 消息序列化/反序列化工具
    // =========================

    /**
     * 反序列化 JSON 字符串为 JSON-RPC 消息（自动识别请求/通知/响应）
     */
    public static JSONRPCMessage deserialize(String json) throws IOException {
        Map<String, Object> map = MAPPER.readValue(json, MAP_TYPE);

        boolean hasMethod = map.containsKey("method");
        boolean hasId = map.containsKey("id");
        boolean hasResult = map.containsKey("result");
        boolean hasError = map.containsKey("error");

        // 响应：包含 id + result/error
        if (hasId && (hasResult || hasError)) {
            return MAPPER.readValue(json, JSONRPCResponse.class);
        }

        // 请求：包含 method + id
        if (hasMethod && hasId) {
            return MAPPER.readValue(json, JSONRPCRequest.class);
        }

        // 通知：仅 method，无 id
        if (hasMethod) {
            return MAPPER.readValue(json, JSONRPCNotification.class);
        }

        throw new IllegalArgumentException("Invalid JSON-RPC message");
    }

    /**
     * 对象序列化为 JSON 字符串
     */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 对象类型安全转换
     */
    public static <T> T convert(Object data, TypeReference<T> type) {
        return data == null ? null : MAPPER.convertValue(data, type);
    }

    /**
     * 快速构建错误响应
     */
    public static JSONRPCResponse ofError(Object id, int code, String message) {
        return JSONRPCResponse.ofError(id, code, message, null);
    }

    // =========================
    // JSON-RPC 消息顶层接口
    // =========================

    /** JSON-RPC 消息顶层标记接口：请求/通知/响应 统一父类型 */
    public interface JSONRPCMessage {
        String jsonrpc();

        /**
         * 统一模式匹配：处理不同消息类型
         */
        default <T> T fold(java.util.function.Function<JSONRPCRequest, T> onRequest,
                           java.util.function.Function<JSONRPCNotification, T> onNotification,
                           java.util.function.Function<JSONRPCResponse, T> onResponse) {
            if (this instanceof JSONRPCRequest req) {
                return onRequest.apply(req);
            } else if (this instanceof JSONRPCNotification noti) {
                return onNotification.apply(noti);
            } else if (this instanceof JSONRPCResponse resp) {
                return onResponse.apply(resp);
            }
            throw new IllegalStateException("Unknown type");
        }
    }

    // =========================
    // JSON-RPC 基础消息结构
    // =========================

    /** JSON-RPC 请求（带 ID，需要响应） */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JSONRPCRequest(String jsonrpc, String method, Object id, Object params) implements JSONRPCMessage {
    }

    /** JSON-RPC 通知（无 ID，无需响应） */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JSONRPCNotification(String jsonrpc, String method, Object params) implements JSONRPCMessage {
    }

    /** JSON-RPC 响应（成功/错误） */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JSONRPCResponse(String jsonrpc, Object id, Object result,
                                  JSONRPCError error) implements JSONRPCMessage {

        /** 构建成功响应 */
        public static JSONRPCResponse ofSuccess(Object id, Object result) {
            return new JSONRPCResponse(JSONRPC_VERSION, id, result, null);
        }

        /** 构建错误响应 */
        public static JSONRPCResponse ofError(Object id, int code, String message, Object data) {
            return new JSONRPCResponse(JSONRPC_VERSION, id, null, new JSONRPCError(code, message, data));
        }

        /** JSON-RPC 错误体 */
        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public record JSONRPCError(int code, String message, Object data) {
        }
    }

    // =========================
    // MCP 核心业务模型
    // =========================

    /** 实现方信息：客户端/服务端名称+版本 */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Implementation(@JsonProperty("name") String name, @JsonProperty("version") String version) {
    }

    /** JSON Schema 结构：用于描述工具入参 */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JsonSchema(@JsonProperty("type") String type,
                             @JsonProperty("properties") Map<String, Object> properties,
                             @JsonProperty("required") List<String> required,
                             @JsonProperty("additionalProperties") Boolean additionalProperties,
                             @JsonProperty("$defs") Map<String, Object> defs,
                             @JsonProperty("definitions") Map<String, Object> definitions) {
    }

    /** MCP 工具定义 */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tool(@JsonProperty("name") String name, @JsonProperty("description") String description,
                       @JsonProperty("inputSchema") JsonSchema inputSchema) {

        /** 字符串 schema 构造 */
        public Tool(String name, String description, String schema) {
            this(name, description, parseSchema(schema));
        }
    }

    /** 工具列表响应结果 */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListToolsResult(@JsonProperty("tools") List<Tool> tools,
                                  @JsonProperty("nextCursor") String nextCursor) {
    }

    /** 解析 JSON 字符串为 JsonSchema */
    private static JsonSchema parseSchema(String schema) {
        try {
            JsonSchema s = MAPPER.readValue(schema, JsonSchema.class);
            if (s.type() == null) {
                throw new IllegalArgumentException("Schema missing type");
            }
            return s;
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid schema", e);
        }
    }

    // =========================
    // MCP 初始化握手
    // =========================

    /** 初始化请求 */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InitializeRequest(@JsonProperty("protocolVersion") String protocolVersion,
                                    @JsonProperty("capabilities") ClientCapabilities capabilities,
                                    @JsonProperty("clientInfo") Implementation clientInfo) {
    }

    /** 初始化响应 */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InitializeResult(@JsonProperty("protocolVersion") String protocolVersion,
                                   @JsonProperty("capabilities") ServerCapabilities capabilities,
                                   @JsonProperty("serverInfo") Implementation serverInfo,
                                   @JsonProperty("instructions") String instructions) {
    }

    // =========================
    // MCP 能力声明
    // =========================

    /** 客户端能力 */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public record ClientCapabilities(Map<String, Object> experimental, RootCapabilities roots, Sampling sampling) {
        public record RootCapabilities(Boolean listChanged) {
        }

        public record Sampling() {
        }
    }

    /** 服务端能力 */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public record ServerCapabilities(CompletionCapabilities completions, Map<String, Object> experimental,
                                     LoggingCapabilities logging, PromptCapabilities prompts,
                                     ResourceCapabilities resources, ToolCapabilities tools) {

        public record CompletionCapabilities() {
        }

        public record LoggingCapabilities() {
        }

        public record PromptCapabilities(Boolean listChanged) {
        }

        public record ResourceCapabilities(Boolean subscribe, Boolean listChanged) {
        }

        public record ToolCapabilities(Boolean listChanged) {
        }
    }
}