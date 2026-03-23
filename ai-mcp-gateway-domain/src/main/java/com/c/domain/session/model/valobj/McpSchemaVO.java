package com.c.domain.session.model.valobj;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONCreator;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.extern.slf4j.Slf4j;

/**
 * MCP/JSON-RPC 2.0协议模型：实现高性能序列化/反序列化
 *
 * @author cyh
 * @date 2026/03/23
 */
@Slf4j
public final class McpSchemaVO {

    /** 私有构造器，禁止实例化 */
    private McpSchemaVO() {
    }

    /**
     * 反序列化JSON-RPC消息
     *
     * @param jsonText JSON格式的RPC消息串
     * @return JSONRPCRequest/JSONRPCResponse
     * @throws IllegalArgumentException 消息格式非法时抛出
     */
    public static JSONRPCMessage deserializeJsonRpcMessage(String jsonText) {
        JSONObject root = JSON.parseObject(jsonText);
        try {
            if (root.containsKey("method")) return root.toJavaObject(JSONRPCRequest.class);
            if (root.containsKey("result") || root.containsKey("error"))
                return root.toJavaObject(JSONRPCResponse.class);
        } catch (Exception e) {
            log.error("JSON-RPC反序列化失败 | 内容: {}", jsonText, e);
            throw new IllegalArgumentException("Invalid JSON-RPC format", e);
        }
        throw new IllegalArgumentException("Unknown JSON-RPC message type");
    }

    /**
     * 序列化JSON-RPC消息（保留Null字段保证协议完整性）
     *
     * @param message RPC消息对象
     * @return 序列化后的JSON字符串
     */
    public static String toJson(Object message) {
        if (message == null) return null;
        if (message instanceof String s) return s; // 防止二次序列化
        return JSON.toJSONString(message, JSONWriter.Feature.WriteMapNullValue, JSONWriter.Feature.BrowserCompatible);
    }

    /**
     * 构建JSON-RPC错误响应
     *
     * @param id      响应ID
     * @param code    错误码（符合JSON-RPC 2.0规范）
     * @param message 错误描述
     * @return 错误响应对象
     */
    public static JSONRPCResponse buildErrorResponse(Object id, int code, String message) {
        return new JSONRPCResponse("2.0", id, null, new JSONRPCResponse.JSONRPCError(code, message, null));
    }

    /**
     * JSON-RPC 2.0消息通用接口
     */
    public sealed interface JSONRPCMessage permits JSONRPCRequest, JSONRPCResponse {
        /** JSON-RPC协议版本（固定2.0） */
        String jsonrpc();
    }

    /**
     * JSON-RPC 2.0请求对象
     *
     * @param jsonrpc 协议版本
     * @param method  调用方法名
     * @param id      请求ID（通知型为null）
     * @param params  调用参数
     */
    public record JSONRPCRequest(@JSONField(name = "jsonrpc") String jsonrpc, @JSONField(name = "method") String method,
                                 @JSONField(name = "id") Object id,
                                 @JSONField(name = "params") Object params) implements JSONRPCMessage {

        @JSONCreator
        public JSONRPCRequest {
        }

        /** 判断是否为通知型请求（无返回ID） */
        public boolean isNotification() {
            return id == null;
        }
    }

    /**
     * JSON-RPC 2.0响应对象
     *
     * @param jsonrpc 协议版本
     * @param id      响应ID
     * @param result  成功结果
     * @param error   错误信息
     */
    public record JSONRPCResponse(@JSONField(name = "jsonrpc") String jsonrpc, @JSONField(name = "id") Object id,
                                  @JSONField(name = "result") Object result,
                                  @JSONField(name = "error") JSONRPCError error) implements JSONRPCMessage {

        @JSONCreator
        public JSONRPCResponse {
        }

        /**
         * JSON-RPC 2.0错误子对象
         *
         * @param code    错误码
         * @param message 错误描述
         * @param data    附加错误信息
         */
        public record JSONRPCError(@JSONField(name = "code") int code, @JSONField(name = "message") String message,
                                   @JSONField(name = "data") Object data) {

            @JSONCreator
            public JSONRPCError {
            }
        }
    }
}