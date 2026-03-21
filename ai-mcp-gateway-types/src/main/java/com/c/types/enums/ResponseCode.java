package com.c.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通用响应码枚举
 * 优化点：增加字段不可变性、静态缓存提升查询性能、语义化判定方法
 *
 * @author cyh
 * @date 2026/03/20
 */
@Getter
@AllArgsConstructor
public enum ResponseCode {

    /** 操作成功 */
    SUCCESS("0000", "成功"),
    /** 未知原因导致的操作失败 */
    UN_ERROR("0001", "未知失败"),
    /** 请求参数不合法或缺失必要参数 */
    ILLEGAL_PARAMETER("0002", "非法参数"),
    /** 请求的方法不存在或未注册 */
    METHOD_NOT_FOUND("0003", "未找到方法"),
    /** 业务处理异常 */
    BUSINESS_ERROR("0004", "业务异常");

    /** 响应状态码 */
    private final String code;
    /** 响应码描述信息 */
    private final String info;

    /**
     * 预热缓存：按 code 映射枚举，提升根据 code 查询的效率 (O(1))
     */
    private static final Map<String, ResponseCode> CACHE = Arrays
            .stream(values())
            .collect(Collectors.toMap(ResponseCode::getCode, Function.identity()));

    /**
     * 根据 code 获取枚举对象
     *
     * @param code 状态码
     * @return 响应码枚举，若不存在则返回 Optional.empty()
     */
    public static Optional<ResponseCode> of(String code) {
        return Optional.ofNullable(CACHE.get(code));
    }

    /**
     * 判定当前状态是否为成功
     *
     * @return true 表示操作成功
     */
    public boolean isSuccess() {
        return SUCCESS.code.equals(this.code);
    }
}