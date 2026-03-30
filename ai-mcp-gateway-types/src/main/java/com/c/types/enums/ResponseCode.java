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
 * 定义系统统一的响应状态码与描述信息，提供枚举查询、成功状态判断能力
 *
 * @author cyh
 * @date 2026/03/30
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
    BUSINESS_ERROR("0004", "业务异常"),

    /** 数据不存在 / 网关配置未找到 */
    DATA_NOT_FOUND("0005", "数据不存在或配置未找到"),

    /** 枚举数据不存在 */
    ENUM_NOT_FOUND("0006", "数据不存在或配置未找到"),

    /** 数据库更新操作失败 */
    DB_UPDATE_FAIL("0007", "数据库更新失败");

    /** 响应状态码 */
    private final String code;
    /** 响应码描述信息 */
    private final String info;

    /** 枚举缓存：根据响应码映射枚举对象，提升查询效率 */
    private static final Map<String, ResponseCode> CACHE = Arrays
            .stream(values())
            .collect(Collectors.toMap(ResponseCode::getCode, Function.identity()));

    /**
     * 根据状态码获取对应的枚举对象
     *
     * @param code 响应状态码
     * @return 匹配的枚举对象包装类，无匹配项时返回空Optional
     */
    public static Optional<ResponseCode> of(String code) {
        return Optional.ofNullable(CACHE.get(code));
    }

    /**
     * 判断当前响应码是否为操作成功状态
     *
     * @return 成功返回true，失败返回false
     */
    public boolean isSuccess() {
        return SUCCESS.code.equals(this.code);
    }
}