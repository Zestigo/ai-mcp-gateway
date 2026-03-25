package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import com.c.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * MCP 日志级别设置处理器
 * 处理logging/setLevel请求，支持动态设置服务端日志输出级别
 *
 * @author cyh
 * @date 2026/03/25
 */
@Slf4j
@Service("loggingHandler")
public class LoggingSetLevelHandler implements IRequestHandler {

    /**
     * 处理日志级别设置请求
     *
     * @param gatewayId 网关唯一标识
     * @param message   JSON-RPC请求消息
     * @return 日志级别设置结果响应
     */
    @Override
    public Flux<McpSchemaVO.JSONRPCResponse> handle(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        // 校验消息类型：仅处理Request请求
        if (!(message instanceof McpSchemaVO.JSONRPCRequest req)) {
            return Flux.error(new AppException("MCP-400", "logging/setLevel 只能处理请求消息"));
        }

        // 默认日志级别为INFO
        String level = "INFO";
        if (req.params() instanceof Map<?, ?> map) {
            Object lv = map.get("level");
            if (lv != null) {
                String candidate = String
                        .valueOf(lv)
                        .trim();
                if (!candidate.isEmpty()) {
                    // 转换为大写格式
                    level = candidate.toUpperCase(Locale.ROOT);
                }
            }
        }

        log.info("logging/setLevel | gatewayId={} | level={}", gatewayId, level);

        // 构建响应结果
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("level", level);

        return Flux.just(McpSchemaVO.JSONRPCResponse.ofSuccess(req.id(), result));
    }
}