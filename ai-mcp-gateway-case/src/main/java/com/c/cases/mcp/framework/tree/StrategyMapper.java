package com.c.cases.mcp.framework.tree;

/**
 * 策略映射器接口
 * 根据请求与上下文自动匹配对应的策略处理器
 *
 * @author cyh
 * @date 2026/03/24
 */
public interface StrategyMapper<T, C, R> {

    /**
     * 获取匹配的策略处理器
     *
     * @param request 请求参数
     * @param context 执行上下文
     * @return 匹配的策略处理器实例
     * @throws Exception 匹配过程异常
     */
    StrategyHandler<T, C, R> get(T request, C context) throws Exception;

}