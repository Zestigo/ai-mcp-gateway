package com.c.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通用响应结果封装类：统一接口返回格式
 *
 * @param <T> 响应数据泛型类型
 * @author cyh
 * @date 2026/03/19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> implements Serializable {

    /** 序列化版本号：保证序列化/反序列化兼容性 */
    private static final long serialVersionUID = 7000723935764546321L;

    /** 响应状态码（如：200成功/500失败） */
    private String code;

    /** 响应信息描述（如：操作成功/系统异常） */
    private String info;

    /** 响应业务数据（泛型适配不同数据类型） */
    private T data;

}