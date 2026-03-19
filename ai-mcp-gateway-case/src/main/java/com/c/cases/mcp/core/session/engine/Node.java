package com.c.cases.mcp.core.session.engine;

import reactor.core.publisher.Mono;

/**
 * MCP会话节点核心接口：定义节点执行逻辑
 *
 * @author cyh
 * @date 2026/03/19
 */
public interface Node {

    /**
     * 执行节点逻辑
     *
     * @param request 请求参数
     * @param context 节点上下文
     * @return 空Mono标识执行完成
     */
    Mono<Void> execute(String request, NodeContext context);

}