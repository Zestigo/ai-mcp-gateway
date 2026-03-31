package com.c.domain.admin.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 领域层通用分页值对象
 * 纯数据载体，用于封装分页查询的页码、条数、总数、数据列表，保证空值安全
 *
 * @author cyh
 * @date 2026/03/31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> implements Serializable {

    /** 序列化ID，保证分布式序列化兼容性 */
    private static final long serialVersionUID = 1L;

    /** 当前页码，从1开始 */
    private int pageNo;

    /** 每页数据条数 */
    private int pageSize;

    /** 数据总记录数 */
    private long total;

    /** 数据列表，确保不为null */
    private List<T> list;

    // ==================== 静态构造方法（分布式安全） ====================

    /**
     * 标准构建分页响应对象
     *
     * @param list     数据列表
     * @param pageNo   当前页码
     * @param pageSize 每页条数
     * @param total    总记录数
     * @return 分页响应实例
     */
    public static <T> PageResponse<T> of(List<T> list, int pageNo, int pageSize, long total) {
        return PageResponse
                .<T>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .total(total)
                .list(list == null ? Collections.emptyList() : list)
                .build();
    }

    /**
     * 构建空分页结果，用于无数据或服务降级场景
     *
     * @return 空数据分页响应实例
     */
    public static <T> PageResponse<T> empty() {
        return PageResponse
                .<T>builder()
                .pageNo(1)
                .pageSize(10)
                .total(0L)
                .list(Collections.emptyList())
                .build();
    }

    // ==================== 安全计算方法（无状态 · 纯计算） ====================

    /**
     * 计算总页数，防除零异常
     *
     * @return 总页数
     */
    public long getTotalPage() {
        if (pageSize <= 0 || total <= 0) {
            return 0L;
        }
        return (total + pageSize - 1) / pageSize;
    }

    /**
     * 判断是否存在下一页
     *
     * @return 存在返回true，不存在返回false
     */
    public boolean isHasNext() {
        return pageNo < getTotalPage();
    }

    /**
     * 判断是否存在上一页
     *
     * @return 存在返回true，不存在返回false
     */
    public boolean isHasPrevious() {
        return pageNo > 1;
    }

    /**
     * 安全判断数据列表是否为空
     *
     * @return 为空返回true，不为空返回false
     */
    public boolean isEmpty() {
        return list == null || list.isEmpty();
    }

}