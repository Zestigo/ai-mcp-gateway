package com.c.domain.session.model.valobj;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

/**
 * MCP协议JSON-RPC消息值对象
 * 提供消息序列化、反序列化、协议结构定义与工具方法
 * 无状态工具类，不可实例化
 */
@Slf4j
public final class McpSchemaVO {

    /** MCP协议最新版本号 */
    public static final String LATEST_PROTOCOL_VERSION = "2024-11-05";

    /** JSON-RPC协议固定版本 */
    public static final String JSONRPC_VERSION = "2.0";

    /** JSON序列化工具实例 */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** MCP请求标记接口，限定请求类型 */
    public sealed interface Request permits InitializeRequest, CallToolRequest {
    }

    /** Map类型引用，解决泛型擦除问题 */
    private static final TypeReference<HashMap<String, Object>> MAP_TYPE_REF = new TypeReference<>() {
    };

    // =========================
    // JSON-RPC 方法常量
    // =========================

    /** JSON-RPC调用方法名常量 */
    public static final class Methods {
        /** 初始化方法 */
        public static final String INITIALIZE = "initialize";
        /** 获取工具列表方法 */
        public static final String LIST_TOOLS = "tools/list";
        /** 调用工具方法 */
        public static final String CALL_TOOL = "tools/call";
    }

    // =========================
    // JSON-RPC 错误码常量
    // =========================

    /** JSON-RPC标准错误码 */
    public static final class ErrorCodes {
        /** JSON解析错误 */
        public static final int PARSE_ERROR = -32700;
        /** 请求格式无效 */
        public static final int INVALID_REQUEST = -32600;
        /** 方法不存在 */
        public static final int METHOD_NOT_FOUND = -32601;
        /** 参数无效 */
        public static final int INVALID_PARAMS = -32602;
        /** 服务内部错误 */
        public static final int INTERNAL_ERROR = -32603;
    }

    // =========================
    // JSON 序列化配置
    // =========================

    /** 全局ObjectMapper，配置宽松解析、忽略未知字段、不序列化空值 */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
            .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /** Map类型引用 */
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    // =========================
    // 序列化工具方法
    // =========================

    /**
     * 将JSON字符串反序列化为JSON-RPC消息对象
     *
     * @param json JSON字符串
     * @return JSON-RPC消息实例
     * @throws IOException 解析异常
     */
    public static JSONRPCMessage deserialize(String json) throws IOException {
        Map<String, Object> map = MAPPER.readValue(json, MAP_TYPE);

        boolean hasMethod = map.containsKey("method");
        boolean hasId = map.containsKey("id");
        boolean hasResult = map.containsKey("result");
        boolean hasError = map.containsKey("error");

        // 响应消息：包含id + result/error
        if (hasId && (hasResult || hasError)) {
            return MAPPER.readValue(json, JSONRPCResponse.class);
        }

        // 请求消息：包含method + id
        if (hasMethod && hasId) {
            return MAPPER.readValue(json, JSONRPCRequest.class);
        }

        // 通知消息：仅包含method，无id
        if (hasMethod) {
            return MAPPER.readValue(json, JSONRPCNotification.class);
        }

        throw new IllegalArgumentException("Invalid JSON-RPC message");
    }

    /**
     * 对象序列化为JSON字符串
     *
     * @param obj 待序列化对象
     * @return JSON字符串
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
     *
     * @param data 源对象
     * @param type 目标类型引用
     * @return 转换后对象
     */
    public static <T> T convert(Object data, TypeReference<T> type) {
        return data == null ? null : MAPPER.convertValue(data, type);
    }

    /**
     * 构建错误响应对象
     *
     * @param id      请求ID
     * @param code    错误码
     * @param message 错误信息
     * @return 错误响应
     */
    public static JSONRPCResponse ofError(Object id, int code, String message) {
        return JSONRPCResponse.ofError(id, code, message, null);
    }

    // =========================
    // JSON-RPC 消息顶层结构
    // =========================

    /** JSON-RPC消息顶层接口 */
    public interface JSONRPCMessage {
        String jsonrpc();

        /**
         * 消息类型匹配处理
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

    /** JSON-RPC请求对象（带ID，需响应） */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JSONRPCRequest(String jsonrpc, String method, Object id, Object params) implements JSONRPCMessage {
    }

    /** JSON-RPC通知对象（无ID，无需响应） */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JSONRPCNotification(String jsonrpc, String method, Object params) implements JSONRPCMessage {
    }

    /** JSON-RPC响应对象 */
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

        /** JSON-RPC错误体 */
        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public record JSONRPCError(int code, String message, Object data) {
        }
    }

    // =========================
    // MCP 基础业务模型
    // =========================

    /** 实现方信息（名称+版本） */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Implementation(String name, String version) {
    }

    /** JSON Schema结构，用于描述入参格式 */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JsonSchema( /** 数据类型，如 object/string/number/boolean */
                              String type,
                              /** 字段属性定义集合 */
                              Map<String, Object> properties,
                              /** 必填字段名称列表 */
                              List<String> required,
                              /** 是否允许额外属性 */
                              Boolean additionalProperties,
                              /** 公共定义集合，JSON Schema标准字段 */
                              @JsonProperty("$defs") Map<String, Object> defs,
                              /** 兼容旧版的定义集合 */
                              Map<String, Object> definitions) {
    }

    /** MCP工具定义 */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tool(String name, String description, JsonSchema inputSchema) {

        /** 通过字符串schema构造工具对象 */
        public Tool(String name, String description, String schema) {
            this(name, description, parseSchema(schema));
        }
    }

    /** 工具列表查询结果 */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListToolsResult(List<Tool> tools, String nextCursor) {
    }

    /**
     * 解析JSON字符串为JsonSchema对象
     */
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
    // MCP 初始化模型
    // =========================

    /** 初始化请求 */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InitializeRequest(String protocolVersion, ClientCapabilities capabilities,
                                    Implementation clientInfo) implements Request {
    }

    /** 初始化响应 */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InitializeResult(String protocolVersion, ServerCapabilities capabilities, Implementation serverInfo,
                                   String instructions) {
    }

    // =========================
    // MCP 能力定义
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
    public record ServerCapabilities(/** 自动补全能力 */
                                     CompletionCapabilities completions,
                                     /** 实验性扩展能力 */
                                     Map<String, Object> experimental,
                                     /** 日志控制能力 */
                                     LoggingCapabilities logging,
                                     /** 提示词模板能力 */
                                     PromptCapabilities prompts,
                                     /** 资源管理能力 */
                                     ResourceCapabilities resources,
                                     /** 工具调用能力 */
                                     ToolCapabilities tools) {
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

    /** 工具调用请求 */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CallToolRequest(String name, Map<String, Object> arguments) implements Request {

        /** 通过JSON字符串参数构造请求 */
        public CallToolRequest(String name, String jsonArguments) {
            this(name, parseJsonArguments(jsonArguments));
        }

        /** 解析JSON参数为Map */
        private static Map<String, Object> parseJsonArguments(String jsonArguments) {
            try {
                return objectMapper.readValue(jsonArguments, MAP_TYPE_REF);
            } catch (IOException e) {
                throw new IllegalArgumentException("Invalid arguments: " + jsonArguments, e);
            }
        }
    }

    /** 私有构造，禁止实例化 */
    private McpSchemaVO() {
    }
}