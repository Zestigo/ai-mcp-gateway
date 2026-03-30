package com.c.api.common;

import com.c.types.enums.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通用响应结果封装类
 * 统一接口返回格式，提供静态工厂方法简化调用
 *
 * @author cyh
 * @date 2026/03/29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> implements Serializable {

    /** 序列化ID，保证序列化兼容性 */
    private static final long serialVersionUID = 7000723935764546321L;

    /** 响应状态码，标识接口处理结果 */
    private String code;

    /** 响应信息描述，对处理结果的文字说明 */
    private String info;

    /** 响应业务数据，接口返回的实际业务对象 */
    private T data;

    /**
     * 构建携带数据的成功响应
     *
     * @param data 响应业务数据
     * @return 成功响应对象
     */
    public static <T> Response<T> success(T data) {
        // 采用建造者模式构建响应对象，使用系统默认成功状态码
        return Response
                .<T>builder()
                .code(ResponseCode.SUCCESS.getCode())     // 设置成功状态码
                .info(ResponseCode.SUCCESS.getInfo())     // 设置成功描述信息
                .data(data)                               // 设置业务返回数据
                .build();                                 // 构建最终响应对象
    }

    /**
     * 构建无数据的成功响应
     * 适用于新增、修改、删除等无返回数据操作
     *
     * @return 成功响应对象
     */
    public static <T> Response<T> success() {
        // 复用带参成功方法，传入null表示无业务数据
        return success(null);
    }

    /**
     * 根据响应枚举构建失败响应
     *
     * @param responseCode 响应状态枚举
     * @return 失败响应对象
     */
    public static <T> Response<T> fail(ResponseCode responseCode) {
        // 从枚举中直接获取状态码与描述，无需手动拼接
        return Response
                .<T>builder()
                .code(responseCode.getCode())    // 从枚举获取错误码
                .info(responseCode.getInfo())    // 从枚举获取错误描述
                .build();                        // 构建错误响应对象
    }

    /**
     * 自定义状态码和信息构建失败响应
     *
     * @param code 自定义状态码
     * @param info 自定义响应信息
     * @return 失败响应对象
     */
    public static <T> Response<T> fail(String code, String info) {
        // 支持业务层自定义错误码与提示信息
        return Response
                .<T>builder()
                .code(code)      // 设置自定义错误码
                .info(info)      // 设置自定义错误提示
                .build();        // 构建响应
    }

    /**
     * 使用系统异常码+自定义信息构建失败响应
     *
     * @param info 异常描述信息
     * @return 失败响应对象
     */
    public static <T> Response<T> fail(String info) {
        // 固定使用系统未知错误码，仅替换提示信息
        return fail(ResponseCode.UN_ERROR.getCode(), info);
    }

}