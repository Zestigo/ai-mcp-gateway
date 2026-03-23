package com.c.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通用响应结果封装类，统一接口返回格式
 *
 * @author cyh
 * @date 2026/03/24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 7000723935764546321L;

    /** 响应状态码 */
    private String code;

    /** 响应信息描述 */
    private String info;

    /** 响应业务数据 */
    private T data;

}