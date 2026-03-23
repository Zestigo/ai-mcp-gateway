package com.c.cases.mcp.framework.routing;

/**
 * 策略处理器函数式接口
 * 定义策略执行的统一入口，支持Lambda实现
 *
 * @author cyh
 * @date 2026/03/24
 */
@FunctionalInterface
public interface StrategyHandler<T, C, R> {

    /**
     * 执行策略逻辑
     *
     * @param request 请求参数
     * @param context 执行上下文
     * @return 策略执行结果
     * @throws Exception 执行过程异常
     */
    R apply(T request, C context) throws Exception;

}