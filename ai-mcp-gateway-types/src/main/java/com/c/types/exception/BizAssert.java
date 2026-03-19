package com.c.types.exception;

import com.c.types.enums.ResponseCode;

import java.util.Objects;

/**
 * 业务断言：简化异常抛出逻辑
 */
public class BizAssert {

    /**
     * 检查对象是否为空，为空则抛出指定异常
     */
    public static void notNull(Object object, ResponseCode responseCode) {
        if (Objects.isNull(object)) {
            throw new AppException(responseCode);
        }
    }

    /**
     * 检查表达式是否为真，为假则抛出指定异常
     */
    public static void isTrue(boolean expression, ResponseCode responseCode) {
        if (!expression) {
            throw new AppException(responseCode);
        }
    }
}