package com.c.cases.mcp.framework.tree;

/**
 * 多线程策略路由抽象类
 * 基于策略模式+责任链实现，提供策略匹配、多线程扩展、统一执行的通用能力
 *
 * @author cyh
 * @date 2026/03/24
 */
public abstract class AbstractMultiThreadStrategyRouter<T, C, R>
        implements StrategyMapper<T, C, R>, StrategyHandler<T, C, R> {

    /**
     * 默认兜底策略：避免未匹配到策略时返回null
     */
    protected StrategyHandler<T, C, R> defaultStrategyHandler = (req, ctx) -> {
        throw new IllegalStateException("No strategy matched.");
    };

    /**
     * 策略路由执行入口
     *
     * @param request 请求参数
     * @param context 执行上下文
     * @return 执行结果
     * @throws Exception 执行异常
     */
    public R router(T request, C context) throws Exception {
        // 获取匹配的策略处理器
        StrategyHandler<T, C, R> handler = get(request, context);
        // 执行策略或使用默认兜底
        return handler != null ? handler.apply(request, context) : defaultStrategyHandler.apply(request, context);
    }

    /**
     * 策略总执行入口
     *
     * @param request 请求参数
     * @param context 执行上下文
     * @return 执行结果
     * @throws Exception 执行异常
     */
    @Override
    public R apply(T request, C context) throws Exception {
        // 执行多线程扩展逻辑
        multiThread(request, context);
        // 执行当前节点核心逻辑
        return doApply(request, context);
    }

    /**
     * 多线程扩展方法，子类可重写实现并发逻辑
     *
     * @param request 请求参数
     * @param context 执行上下文
     * @throws Exception 扩展逻辑异常
     */
    protected void multiThread(T request, C context) throws Exception {
        // 默认空实现，由子类按需扩展
    }

    /**
     * 节点核心业务逻辑，由子类实现
     *
     * @param request 请求参数
     * @param context 执行上下文
     * @return 执行结果
     * @throws Exception 业务异常
     */
    protected abstract R doApply(T request, C context) throws Exception;
}