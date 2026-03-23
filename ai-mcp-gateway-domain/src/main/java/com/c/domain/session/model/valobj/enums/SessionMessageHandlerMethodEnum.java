package com.c.domain.session.model.valobj.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MCP/JSON-RPC请求方法路由枚举：映射方法名到处理器
 *
 * @author cyh
 * @date 2026/03/23
 */
@Getter
@AllArgsConstructor
public enum SessionMessageHandlerMethodEnum {

    // 核心生命周期指令
    INITIALIZE("initialize", "initializeHandler", "初始化请求"),
    INITIALIZED("notifications/initialized", "initializedHandler", "初始化完成通知"),

    // 工具类指令
    TOOLS_LIST("tools/list", "toolsListHandler", "查询工具列表"),
    TOOLS_CALL("tools/call", "toolsCallHandler", "执行工具调用"),

    // 资源类指令
    RESOURCES_LIST("resources/list", "resourcesListHandler", "查询资源列表"),
    RESOURCES_READ("resources/read", "resourcesReadHandler", "读取资源内容"),

    // 辅助指令
    LOGGING_SET_LEVEL("logging/setLevel", "loggingHandler", "设置日志级别"),
    ROOTS_LIST("roots/list", "rootsListHandler", "列出根目录");

    private final String method;       // RPC方法名
    private final String handlerName;  // 处理器Bean名称
    private final String description;  // 方法描述

    /** 方法名-枚举缓存池：O(1)高效匹配 */
    private static final Map<String, SessionMessageHandlerMethodEnum> METHOD_CACHE = Arrays
            .stream(values())
            .collect(Collectors.toMap(SessionMessageHandlerMethodEnum::getMethod, Function.identity(), (existing,
                                                                                                        replacement) -> existing // 冲突保留前者
            ));

    /**
     * 根据方法名获取路由枚举
     *
     * @param method RPC方法名
     * @return 匹配的枚举（Optional封装）
     */
    public static Optional<SessionMessageHandlerMethodEnum> getByMethod(String method) {
        if (method == null || method.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(METHOD_CACHE.get(method.trim()));
    }

    /**
     * 判断是否支持指定MCP方法
     *
     * @param method RPC方法名
     * @return true=支持，false=不支持
     */
    public static boolean isSupported(String method) {
        return method != null && METHOD_CACHE.containsKey(method.trim());
    }
}