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

    // =========================
    // 全局协议常量
    // =========================
    /** MCP协议最新版本号 */
    public static final String LATEST_PROTOCOL_VERSION = "2024-11-05";

    /** JSON-RPC协议固定版本 */
    public static final String JSONRPC_VERSION = "2.0";

    // =========================
    // 序列化工具配置
    // =========================
    /** 基础JSON序列化工具实例 */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 协议专用序列化器
     * 静态常量单例，不依赖Spring注入
     * 忽略未知字段、不序列化null值、禁用日期时间戳
     */
    private static final ObjectMapper PROTOCOL_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    /**
     * 全局通用序列化器
     * 宽松解析、浮点转BigDecimal、空字符串转null、不序列化null值
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
            .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    // =========================
    // 泛型类型引用
    // =========================
    /** HashMap类型引用，解决泛型擦除问题 */
    private static final TypeReference<HashMap<String, Object>> MAP_TYPE_REF = new TypeReference<>() {
    };

    /** Map类型引用，通用反序列化适配 */
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    // =========================
    // MCP请求标记接口
    // =========================

    /** MCP请求标记接口，限定所有请求类型实现 */
    public sealed interface Request permits InitializeRequest, CallToolRequest {
    }

    // =========================
    // 核心序列化/反序列化方法
    // =========================

    /**
     * 对象序列化为JSON字符串
     *
     * @param obj 待序列化对象
     * @return 序列化后的JSON字符串
     * @throws RuntimeException 序列化失败时抛出
     */
    public static String toJson(Object obj) {
        try {
            return PROTOCOL_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("MCP 协议序列化失败", e);
        }
    }

    /**
     * JSON字符串反序列化为指定类型对象
     *
     * @param json  JSON字符串
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 反序列化后的对象
     * @throws RuntimeException 反序列化失败时抛出
     */
    public static <T> T deserialize(String json, Class<T> clazz) {
        try {
            return PROTOCOL_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("MCP 协议解析失败", e);
        }
    }

    /**
     * 高性能JSON-RPC消息反序列化分发器
     * 使用JsonNode提升解析效率，按协议规范自动识别消息类型
     *
     * @param jsonText 原始JSON消息
     * @return 匹配的JSON-RPC消息对象
     * @throws IOException              IO解析异常
     * @throws IllegalArgumentException 消息为空或格式非法时抛出
     */
    public static JSONRPCMessage deserializeJsonRpcMessage(String jsonText) throws IOException {
        if (jsonText == null || jsonText.isBlank()) {
            throw new IllegalArgumentException("JSON content is empty");
        }

        log.debug("Received JSON-RPC message: {}", jsonText);

        // 解析为轻量级树状结构，避免全量对象实例化开销
        JsonNode rootNode = objectMapper.readTree(jsonText);

        // 包含method字段：请求/通知
        if (rootNode.has("method")) {
            if (rootNode.has("id")) {
                // 包含id：标准请求
                return objectMapper.treeToValue(rootNode, JSONRPCRequest.class);
            } else {
                // 无id：通知消息
                return objectMapper.treeToValue(rootNode, JSONRPCNotification.class);
            }
        }

        // 包含result/error字段：响应消息
        if (rootNode.has("result") || rootNode.has("error")) {
            return objectMapper.treeToValue(rootNode, JSONRPCResponse.class);
        }

        // 无法识别的协议格式
        throw new IllegalArgumentException("Unknown or invalid JSON-RPC message format: " + jsonText);
    }

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

        // 响应消息：id + result/error
        if (hasId && (hasResult || hasError)) {
            return MAPPER.readValue(json, JSONRPCResponse.class);
        }

        // 请求消息：method + id
        if (hasMethod && hasId) {
            return MAPPER.readValue(json, JSONRPCRequest.class);
        }

        // 通知消息：仅method
        if (hasMethod) {
            return MAPPER.readValue(json, JSONRPCNotification.class);
        }

        throw new IllegalArgumentException("Invalid JSON-RPC message");
    }

    /**
     * 对象类型安全转换
     *
     * @param data 源对象
     * @param type 目标类型引用
     * @param <T>  目标泛型类型
     * @return 转换后对象，源对象为空时返回null
     */
    public static <T> T convert(Object data, TypeReference<T> type) {
        return data == null ? null : MAPPER.convertValue(data, type);
    }

    /**
     * 快速构建JSON-RPC错误响应
     *
     * @param id      请求ID
     * @param code    错误码
     * @param message 错误描述
     * @return 标准错误响应对象
     */
    public static JSONRPCResponse ofError(Object id, int code, String message) {
        return JSONRPCResponse.ofError(id, code, message, null);
    }

    // =========================
    // 协议常量定义
    // =========================

    /** JSON-RPC调用方法名常量集合 */
    public static final class Methods {
        /** 初始化方法 */
        public static final String INITIALIZE = "initialize";
        /** 获取工具列表方法 */
        public static final String LIST_TOOLS = "tools/list";
        /** 调用工具方法 */
        public static final String CALL_TOOL = "tools/call";
    }

    /** JSON-RPC标准错误码常量集合 */
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
    // JSON-RPC 协议基础模型
    // =========================

    /** JSON-RPC消息顶层接口，所有消息类型统一实现 */
    public interface JSONRPCMessage {
        String jsonrpc();

        /**
         * 消息类型匹配处理，支持请求/通知/响应三种类型的分支处理
         *
         * @param onRequest      请求消息处理函数
         * @param onNotification 通知消息处理函数
         * @param onResponse     响应消息处理函数
         * @param <T>            返回值类型
         * @return 处理结果
         * @throws IllegalStateException 未知消息类型时抛出
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

    /** JSON-RPC请求对象（带ID，需要服务端响应） */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JSONRPCRequest(String jsonrpc, String method, Object id, Object params) implements JSONRPCMessage {
    }

    /** JSON-RPC通知对象（无ID，无需服务端响应） */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JSONRPCNotification(String jsonrpc, String method, Object params) implements JSONRPCMessage {
    }

    /** JSON-RPC响应对象 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JSONRPCResponse(String jsonrpc, Object id, Object result,
                                  JSONRPCError error) implements JSONRPCMessage {

        /**
         * 构建成功响应
         *
         * @param id     请求ID
         * @param result 业务结果
         * @return 成功响应
         */
        public static JSONRPCResponse ofSuccess(Object id, Object result) {
            return new JSONRPCResponse(JSONRPC_VERSION, id, result, null);
        }

        /**
         * 构建错误响应
         *
         * @param id      请求ID
         * @param code    错误码
         * @param message 错误信息
         * @param data    扩展错误数据
         * @return 错误响应
         */
        public static JSONRPCResponse ofError(Object id, int code, String message, Object data) {
            return new JSONRPCResponse(JSONRPC_VERSION, id, null, new JSONRPCError(code, message, data));
        }

        /** JSON-RPC错误体结构 */
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

    /** JSON Schema结构，用于描述接口入参格式 */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JsonSchema(
            /** 数据类型，如 object/string/number/boolean */
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

        /**
         * 通过JSON字符串schema构造工具对象
         *
         * @param name        工具名称
         * @param description 工具描述
         * @param schema      JSON格式的入参schema字符串
         */
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
     *
     * @param schema JSON schema字符串
     * @return 解析后的JsonSchema对象
     * @throws IllegalArgumentException schema格式非法或缺少必要字段时抛出
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
    // MCP 初始化相关模型
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
    // MCP 客户端/服务端能力模型
    // =========================

    /** 客户端能力 */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public record ClientCapabilities(Map<String, Object> experimental, RootCapabilities roots, Sampling sampling) {
        /** 客户端根目录能力 */
        public record RootCapabilities(Boolean listChanged) {
        }

        /** 客户端采样能力 */
        public record Sampling() {
        }
    }

    /** 服务端能力 */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public record ServerCapabilities(
            /** 自动补全能力 */
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

        /** 补全能力 */
        public record CompletionCapabilities() {
        }

        /** 日志能力 */
        public record LoggingCapabilities() {
        }

        /** 提示词能力 */
        public record PromptCapabilities(Boolean listChanged) {
        }

        /** 资源能力 */
        public record ResourceCapabilities(Boolean subscribe, Boolean listChanged) {
        }

        /** 工具能力 */
        public record ToolCapabilities(Boolean listChanged) {
        }
    }

    // =========================
    // MCP 工具调用模型
    // =========================

    /** 工具调用请求 */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CallToolRequest(String name, Map<String, Object> arguments) implements Request {

        /**
         * 通过JSON字符串参数构造工具调用请求
         *
         * @param name          工具名称
         * @param jsonArguments JSON格式的参数字符串
         */
        public CallToolRequest(String name, String jsonArguments) {
            this(name, parseJsonArguments(jsonArguments));
        }

        /**
         * 解析JSON格式参数为Map集合
         *
         * @param jsonArguments JSON参数字符串
         * @return 解析后的参数Map
         * @throws IllegalArgumentException 参数格式非法时抛出
         */
        private static Map<String, Object> parseJsonArguments(String jsonArguments) {
            try {
                return objectMapper.readValue(jsonArguments, MAP_TYPE_REF);
            } catch (IOException e) {
                throw new IllegalArgumentException("Invalid arguments: " + jsonArguments, e);
            }
        }
    }

    /**
     * 私有构造方法，禁止工具类实例化
     */
    private McpSchemaVO() {
    }
}