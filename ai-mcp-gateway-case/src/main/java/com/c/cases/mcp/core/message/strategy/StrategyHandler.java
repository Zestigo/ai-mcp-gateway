package com.c.cases.mcp.core.message.strategy;

/**
 * 策略处理器函数式接口：定义策略执行的通用方法（单方法接口）
 *
 * @param <T> 请求参数类型
 * @param <C> 上下文类型
 * @param <R> 返回值类型
 * @author cyh
 * @date 2026/03/19
 */
@FunctionalInterface
public interface StrategyHandler<T, C, R> {

    /**
     * 执行策略逻辑
     *
     * @param request 请求参数
     * @param context 执行上下文
     * @return 策略执行结果
     * @throws Exception 执行过程中抛出的异常
     */
    R apply(T request, C context) throws Exception;

}