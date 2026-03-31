package com.c.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 全局统一响应码枚举
 * 定义系统所有接口、服务、业务流程的标准响应码与描述信息
 * 提供枚举快速查询、成功状态判断等通用能力
 *
 * @author cyh
 * @date 2026-03-31
 */
@Getter
@AllArgsConstructor
public enum ResponseCode {

    /** 操作成功 */
    SUCCESS("0000", "成功"),
    /** 系统未知异常 */
    UN_ERROR("0001", "系统未知异常"),
    /** 请求参数非法或缺失 */
    ILLEGAL_PARAMETER("0002", "请求参数非法"),
    /** 接口方法不存在 */
    METHOD_NOT_FOUND("0003", "方法不存在"),
    /** 业务逻辑处理异常 */
    BUSINESS_ERROR("0004", "业务处理异常"),
    /** 查询数据不存在 */
    DATA_NOT_FOUND("0005", "数据不存在"),
    /** 枚举类型未定义 */
    ENUM_NOT_FOUND("0006", "枚举类型不存在"),
    /** 数据库更新操作失败 */
    DB_UPDATE_FAIL("0007", "数据更新失败"),
    /** 并发冲突（乐观锁/分布式锁） */
    CONCURRENT_ERROR("0008", "操作冲突，请稍后重试"),
    /** 无权限访问资源 */
    NO_PERMISSION("0009", "无访问权限"),
    /** 重复提交/重复操作 */
    REPEAT_OPERATE("0010", "请勿重复操作");

    /** 响应状态码 */
    private final String code;
    /** 响应信息描述 */
    private final String info;

    /** 响应码缓存映射：code -> 枚举对象，用于快速查询 */
    private static final Map<String, ResponseCode> CACHE = Arrays
            .stream(values())
            .collect(Collectors.toMap(ResponseCode::getCode, Function.identity()));

    /**
     * 根据响应码获取枚举对象
     *
     * @param code 响应码字符串
     * @return 对应的 ResponseCode 枚举，不存在则返回 null
     */
    public static ResponseCode getByCode(String code) {
        return CACHE.get(code);
    }

    /**
     * 判断当前响应是否为成功状态
     *
     * @return 成功返回 true，失败返回 false
     */
    public boolean isSuccess() {
        return SUCCESS == this;
    }

}