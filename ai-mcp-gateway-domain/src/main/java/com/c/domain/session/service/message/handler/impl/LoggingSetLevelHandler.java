package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * logging/setLevel 处理器
 *
 * @author cyh
 * @date 2026/03/24
 */
@Slf4j
@Service("loggingHandler")
public class LoggingSetLevelHandler implements IRequestHandler {

    /**
     * 处理日志级别设置
     *
     * @param gatewayId 网关标识
     * @param message   请求消息
     * @return 响应结果
     */
    @Override
    public McpSchemaVO.JSONRPCResponse handle(String gatewayId, McpSchemaVO.JSONRPCRequest message) {
        // 默认日志级别
        String level = "INFO";
        // 从参数中获取级别
        if (message.params() instanceof Map<?, ?> paramMap) {
            Object lv = paramMap.get("level");
            if (lv != null) {
                level = String.valueOf(lv);
            }
        }

        log.info("处理 logging/setLevel | gatewayId={} | level={}", gatewayId, level);

        // 构造响应结果
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("level", level);

        return new McpSchemaVO.JSONRPCResponse("2.0", message.id(), result, null);
    }
}