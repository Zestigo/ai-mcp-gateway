package com.c.cases.mcp.core.message.strategy;

/**
 * 策略映射器接口：定义获取策略处理器的标准
 *
 * @param <T> 请求参数类型
 * @param <C> 上下文类型
 * @param <R> 返回值类型
 * @author cyh
 * @date 2026/03/19
 */
public interface StrategyMapper<T, C, R> {

    /**
     * 获取策略处理器
     *
     * @param request 请求参数
     * @param context 执行上下文
     * @return 匹配的策略处理器
     * @throws Exception 获取过程中抛出的异常
     */
    StrategyHandler<T, C, R> get(T request, C context) throws Exception;

}