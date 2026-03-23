package com.c.domain.session.model.valobj;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONCreator;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.extern.slf4j.Slf4j;

/**
 * MCP / JSON-RPC 2.0 协议模型
 *
 * @author cyh
 * @date 2026/03/24
 */
@Slf4j
public final class McpSchemaVO {

    /**
     * 私有构造，禁止实例化
     */
    private McpSchemaVO() {
    }

    /**
     * 反序列化 JSON-RPC 消息
     *
     * @param jsonText JSON 字符串
     * @return JSON-RPC 消息对象
     */
    public static JSONRPCMessage deserializeJsonRpcMessage(String jsonText) {
        // 空内容直接抛出异常
        if (jsonText == null || jsonText.isBlank()) {
            throw new IllegalArgumentException("JSON-RPC payload is blank");
        }

        try {
            // 解析为JSON对象
            JSONObject root = JSON.parseObject(jsonText);
            // 包含method字段为请求消息
            if (root.containsKey("method")) {
                return root.toJavaObject(JSONRPCRequest.class);
            }
            // 包含result/error字段为响应消息
            if (root.containsKey("result") || root.containsKey("error")) {
                return root.toJavaObject(JSONRPCResponse.class);
            }
        } catch (Exception e) {
            // 反序列化异常记录日志并抛出
            log.error("JSON-RPC 反序列化失败 | 内容: {}", jsonText, e);
            throw new IllegalArgumentException("Invalid JSON-RPC format", e);
        }

        // 无法识别消息类型
        throw new IllegalArgumentException("Unknown JSON-RPC message type");
    }

    /**
     * 对象转 JSON 字符串
     *
     * @param message 待序列化对象
     * @return JSON 字符串
     */
    public static String toJson(Object message) {
        // 空值直接返回null
        if (message == null) return null;
        // 字符串类型直接返回
        if (message instanceof String s) return s;
        // 序列化并保留null值，兼容浏览器
        return JSON.toJSONString(message,
                JSONWriter.Feature.WriteMapNullValue,
                JSONWriter.Feature.BrowserCompatible);
    }

    /**
     * 构建错误响应
     *
     * @param id      请求ID
     * @param code    错误码
     * @param message 错误信息
     * @return 错误响应
     */
    public static JSONRPCResponse buildErrorResponse(Object id, int code, String message) {
        // 构造标准JSON-RPC错误响应
        return new JSONRPCResponse("2.0", id, null, new JSONRPCResponse.JSONRPCError(code, message, null));
    }

    /** JSON-RPC 消息接口 */
    public sealed interface JSONRPCMessage permits JSONRPCRequest, JSONRPCResponse {
        String jsonrpc();
    }

    /** JSON-RPC 请求对象 */
    public record JSONRPCRequest(
            @JSONField(name = "jsonrpc") String jsonrpc,
            @JSONField(name = "method") String method,
            @JSONField(name = "id") Object id,
            @JSONField(name = "params") Object params
    ) implements JSONRPCMessage {

        @JSONCreator
        public JSONRPCRequest {
        }

        /**
         * 判断是否为通知消息
         * 通知消息无ID，无需响应
         */
        public boolean isNotification() {
            return id == null;
        }
    }

    /** JSON-RPC 响应对象 */
    public record JSONRPCResponse(
            @JSONField(name = "jsonrpc") String jsonrpc,
            @JSONField(name = "id") Object id,
            @JSONField(name = "result") Object result,
            @JSONField(name = "error") JSONRPCError error
    ) implements JSONRPCMessage {

        @JSONCreator
        public JSONRPCResponse {
        }

        /** JSON-RPC 错误对象 */
        public record JSONRPCError(
                @JSONField(name = "code") int code,
                @JSONField(name = "message") String message,
                @JSONField(name = "data") Object data
        ) {
            @JSONCreator
            public JSONRPCError {
            }
        }
    }
}