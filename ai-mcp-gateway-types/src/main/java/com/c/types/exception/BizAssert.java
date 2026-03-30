package com.c.types.exception;

import com.c.types.enums.ResponseCode;

import java.util.Objects;

/**
 * 业务断言工具类
 * 提供统一的业务参数校验、状态校验能力，简化异常抛出逻辑
 *
 * @author cyh
 * @date 2026/03/30
 */
public class BizAssert {

    /**
     * 校验对象非空
     * 对象为null时抛出应用异常
     *
     * @param object       待校验的对象
     * @param responseCode 响应码枚举
     */
    public static void notNull(Object object, ResponseCode responseCode) {
        if (Objects.isNull(object)) {
            throw new AppException(responseCode);
        }
    }

    /**
     * 校验表达式为真
     * 表达式为false时抛出应用异常
     *
     * @param expression   待校验的布尔表达式
     * @param responseCode 响应码枚举
     */
    public static void isTrue(boolean expression, ResponseCode responseCode) {
        if (!expression) {
            throw new AppException(responseCode);
        }
    }
}