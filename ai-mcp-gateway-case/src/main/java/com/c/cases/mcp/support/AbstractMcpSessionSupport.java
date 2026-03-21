package com.c.cases.mcp.support;

import com.c.cases.mcp.core.session.factory.DefaultMcpSessionFactory;
import com.c.cases.mcp.framework.router.AbstractMultiThreadStrategyRouter;
import com.c.domain.session.service.management.SessionManagementService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * MCP会话抽象支撑类：继承多线程策略路由类，封装会话管理服务依赖
 *
 * @author cyh
 * @date 2026/03/19
 */
@Slf4j
public abstract class AbstractMcpSessionSupport extends AbstractMultiThreadStrategyRouter<String,
        DefaultMcpSessionFactory.DynamicContext, Flux<ServerSentEvent<String>>> {

    /** 会话管理服务接口（子类可直接使用） */
    @Resource
    protected SessionManagementService sessionManagementService;

    /**
     * 重写多线程执行方法：默认空实现（子类可按需扩展）
     *
     * @param request 请求参数
     * @param context 会话动态上下文
     * @throws ExecutionException   执行异常
     * @throws InterruptedException 中断异常
     * @throws TimeoutException     超时异常
     */
    @Override
    protected void multiThread(String request, DefaultMcpSessionFactory.DynamicContext context) throws ExecutionException, InterruptedException, TimeoutException {
        // 默认空实现（子类可扩展）
    }
}