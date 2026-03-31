package com.c.types.utils;

import com.c.types.enums.ResponseCode;
import com.c.types.exception.AppException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Objects;

/**
 * 业务断言工具类
 * 统一参数校验、状态校验，抛出自定义 AppException
 * @author cyh
 * @date 2026-03-31
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BizAssert {

    // ====================== 基础非空/非空白校验 ======================

    /**
     * 断言对象非 null
     */
    public static void notNull(@Nullable Object obj, String message) {
        if (obj == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), message);
        }
    }

    /**
     * 断言字符串非 null 且非空白（trim 后长度 > 0）
     */
    public static void notBlank(@Nullable CharSequence text, String message) {
        if (!StringUtils.hasText(text)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), message);
        }
    }

    /**
     * 断言集合非 null 且非空
     */
    public static void notEmpty(@Nullable Collection<?> coll, String message) {
        if (CollectionUtils.isEmpty(coll)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), message);
        }
    }

    // ====================== 布尔条件校验 ======================

    /**
     * 断言条件为 true，否则抛业务异常
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new AppException(ResponseCode.BUSINESS_ERROR.getCode(), message);
        }
    }

    /**
     * 断言条件为 false，否则抛业务异常
     */
    public static void isFalse(boolean condition, String message) {
        isTrue(!condition, message);
    }

    // ====================== 等值/不等值校验 ======================

    /**
     * 断言两个对象相等（equals）
     */
    public static void equals(Object a, Object b, String message) {
        isTrue(Objects.equals(a, b), message);
    }

    /**
     * 断言两个对象不相等
     */
    public static void notEquals(Object a, Object b, String message) {
        isTrue(!Objects.equals(a, b), message);
    }

    // ====================== 数值范围校验 ======================

    /**
     * 断言数值 > 0
     */
    public static void gtZero(Number num, String message) {
        isTrue(num != null && num.doubleValue() > 0, message);
    }

    /**
     * 断言数值 >= 0
     */
    public static void geZero(Number num, String message) {
        isTrue(num != null && num.doubleValue() >= 0, message);
    }

    /**
     * 断言数值在 [min, max] 闭区间内
     */
    public static void between(Number num, Number min, Number max, String message) {
        isTrue(num != null
                        && num.doubleValue() >= min.doubleValue()
                        && num.doubleValue() <= max.doubleValue(),
                message);
    }

    // ====================== 数据存在性/状态校验 ======================

    /**
     * 断言数据存在（非 null），否则抛 数据不存在 异常
     */
    public static void exist(@Nullable Object data, String message) {
        if (data == null) {
            throw new AppException(ResponseCode.DATA_NOT_FOUND.getCode(), message);
        }
    }

    /**
     * 断言数据不存在（null），否则抛 数据已存在 异常
     */
    public static void notExist(@Nullable Object data, String message) {
        if (data != null) {
            throw new AppException(ResponseCode.BUSINESS_ERROR.getCode(), message);
        }
    }

    // ====================== 自定义异常码重载（灵活扩展） ======================

    /**
     * 自定义异常码的 notNull
     */
    public static void notNull(@Nullable Object obj, ResponseCode code, String message) {
        if (obj == null) {
            throw new AppException(code.getCode(), message);
        }
    }

    /**
     * 自定义异常码的 isTrue
     */
    public static void isTrue(boolean condition, ResponseCode code, String message) {
        if (!condition) {
            throw new AppException(code.getCode(), message);
        }
    }
}