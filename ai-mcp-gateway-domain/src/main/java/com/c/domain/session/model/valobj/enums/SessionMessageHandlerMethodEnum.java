package com.c.domain.session.model.valobj.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JSON-RPC 方法路由枚举
 *
 * @author cyh
 * @date 2026/03/24
 */
@Getter
@AllArgsConstructor
public enum SessionMessageHandlerMethodEnum {

    INITIALIZE("initialize", "initializeHandler", "初始化请求"),
    INITIALIZED("notifications/initialized", "initializedHandler", "初始化完成通知"),
    TOOLS_LIST("tools/list", "toolsListHandler", "查询工具列表"),
    TOOLS_CALL("tools/call", "toolsCallHandler", "执行工具调用"),
    RESOURCES_LIST("resources/list", "resourcesListHandler", "查询资源列表"),
    RESOURCES_READ("resources/read", "resourcesReadHandler", "读取资源内容"),
    LOGGING_SET_LEVEL("logging/setLevel", "loggingHandler", "设置日志级别"),
    ROOTS_LIST("roots/list", "rootsListHandler", "列出根目录");

    /** 方法名 */
    private final String method;
    /** 处理器名称 */
    private final String handlerName;
    /** 方法描述 */
    private final String description;

    // 方法名与枚举的缓存映射，提升查询效率
    private static final Map<String, SessionMessageHandlerMethodEnum> METHOD_CACHE = Arrays
            .stream(values())
            .collect(Collectors.toMap(SessionMessageHandlerMethodEnum::getMethod, Function.identity(), (a, b) -> a));

    /**
     * 根据方法名获取枚举
     *
     * @param method 方法名
     * @return 匹配的枚举
     */
    public static Optional<SessionMessageHandlerMethodEnum> getByMethod(String method) {
        // 空值直接返回空
        if (method == null || method.isBlank()) {
            return Optional.empty();
        }
        // 从缓存中获取匹配的枚举
        return Optional.ofNullable(METHOD_CACHE.get(method.trim()));
    }

    /**
     * 判断方法是否支持
     *
     * @param method 方法名
     * @return 支持返回 true
     */
    public static boolean isSupported(String method) {
        // 校验方法名是否存在于缓存中
        return method != null && METHOD_CACHE.containsKey(method.trim());
    }
}