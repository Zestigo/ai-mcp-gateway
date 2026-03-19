package com.c.cases.mcp.framework.router;

import com.c.cases.mcp.core.message.strategy.StrategyHandler;
import com.c.cases.mcp.core.message.strategy.StrategyMapper;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * 多线程策略路由抽象类：封装策略路由+并发执行通用逻辑
 *
 * @param <T> 请求参数类型
 * @param <C> 上下文类型
 * @param <R> 返回值类型
 * @author cyh
 * @date 2026/03/19
 */
public abstract class AbstractMultiThreadStrategyRouter<T, C, R> implements StrategyMapper<T, C, R>,
        StrategyHandler<T, C, R> {

    /** 默认策略处理器：无匹配处理器时兜底返回null */
    protected StrategyHandler<T, C, R> defaultStrategyHandler = (req, ctx) -> null;

    /**
     * 路由执行：匹配策略处理器并执行
     *
     * @param request 请求参数
     * @param context 执行上下文
     * @return 策略执行结果
     * @throws Exception 路由/执行过程中抛出的异常
     */
    public R router(T request, C context) throws Exception {
        StrategyHandler<T, C, R> handler = get(request, context);
        return handler != null ? handler.apply(request, context) : defaultStrategyHandler.apply(request, context);
    }

    /**
     * 总执行入口：先执行并发逻辑，再执行节点核心逻辑
     *
     * @param request 请求参数
     * @param context 执行上下文
     * @return 节点执行结果
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    public R apply(T request, C context) throws Exception {
        // ① 并发扩展点（可选）
        multiThread(request, context);
        // ② 当前节点执行
        return doApply(request, context);
    }

    /**
     * 并发处理（可选实现）
     *
     * @param request 请求参数
     * @param context 执行上下文
     * @throws ExecutionException   执行异常
     * @throws InterruptedException 中断异常
     * @throws TimeoutException     超时异常
     */
    protected abstract void multiThread(T request, C context) throws ExecutionException, InterruptedException,
            TimeoutException;

    /**
     * 当前节点核心逻辑（子类必须实现）
     *
     * @param request 请求参数
     * @param context 执行上下文
     * @return 节点执行结果
     * @throws Exception 执行过程中抛出的异常
     */
    protected abstract R doApply(T request, C context) throws Exception;
}