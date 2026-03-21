package com.c.domain.session.model.valobj.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 请求方法枚举策略
 * 优化：增加静态缓存、提高查询效率、增强入参校验
 *
 * @author cyh
 * @date 2026/03/20
 */
@Getter
@AllArgsConstructor
public enum SessionMessageHandlerMethodEnum {

    INITIALIZE("initialize", "initializeHandler", "初始化请求"),
    TOOLS_LIST("tools/list", "toolsListHandler", "工具列表请求"),
    TOOLS_CALL("tools/call", "toolsCallHandler", "工具调用请求"),
    RESOURCES_LIST("resources/list", "resourcesListHandler", "资源列表请求");

    private final String method;
    private final String handlerName;
    private final String description;

    /**
     * 静态缓存 Map：Key 是方法名字符串 (如 "initialize")，Value 是枚举实例本身
     * 原理：在类加载阶段只执行一次，后续查询复杂度为 O(1)
     */
    private static final Map<String, SessionMessageHandlerMethodEnum> METHOD_CACHE = Arrays
            // 1. 将枚举的所有实例（values()）转换成顺序流（Stream），就像把零件放上传送带
            .stream(values())
            // 2. 将流中的元素收集（collect）到指定的数据结构中
            .collect(Collectors.toMap(
                    // 3. 定义 Map 的 Key：调用枚举实例的 getMethod 方法（例如拿取 "tools/call"）
                    SessionMessageHandlerMethodEnum::getMethod,
                    // 4. 定义 Map 的 Value：Function.identity() 表示取枚举实例“自己”
                    // 此时 Map 中存的就是： "tools/call" -> TOOLS_CALL(对象)
                    Function.identity()));

    /**
     * 根据方法名获取枚举
     *
     * @param method 方法名字符串
     * @return 对应的枚举包装在 Optional 中
     */
    public static Optional<SessionMessageHandlerMethodEnum> getByMethod(String method) {
        if (method == null || method.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(METHOD_CACHE.get(method));
    }

    /**
     * 判断是否支持该方法
     */
    public static boolean contains(String method) {
        return METHOD_CACHE.containsKey(method);
    }
}