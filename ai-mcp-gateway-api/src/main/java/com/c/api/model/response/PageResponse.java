package com.c.api.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 通用分页响应对象
 * 用于统一封装接口分页返回的总条数、页码、数据列表等信息
 *
 * @author cyh
 * @date 2026/03/31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> implements Serializable {

    /** 序列化ID，保证序列化兼容性 */
    private static final long serialVersionUID = 1L;

    /** 数据总条数，前端计算总页数的依据 */
    private long total;

    /** 当前页码，从1开始 */
    private int pageNo;

    /** 每页数据条数 */
    private int pageSize;

    /** 当前页数据列表 */
    private List<T> list;

    /**
     * 创建空分页结果
     *
     * @return 空数据的分页响应对象
     */
    public static <T> PageResponse<T> empty() {
        return PageResponse
                .<T>builder()
                .total(0L)
                .pageNo(1)
                .pageSize(10)
                .list(Collections.emptyList())
                .build();
    }

    /**
     * 快速构建分页响应对象
     *
     * @param total    总条数
     * @param pageNo   当前页码
     * @param pageSize 每页条数
     * @param list     数据列表
     * @return 分页响应实例
     */
    public static <T> PageResponse<T> of(long total, int pageNo, int pageSize, List<T> list) {
        return new PageResponse<>(total, pageNo, pageSize, list);
    }

}