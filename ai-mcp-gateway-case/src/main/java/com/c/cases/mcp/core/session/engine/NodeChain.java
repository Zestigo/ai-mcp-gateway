package com.c.cases.mcp.core.session.engine;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * MCP会话节点链：按顺序执行节点列表中的所有节点逻辑
 *
 * @author cyh
 * @date 2026/03/19
 */
public class NodeChain {

    /** 待执行的节点列表 */
    private final List<Node> nodes;

    /**
     * 初始化节点链
     *
     * @param nodes 节点列表（执行顺序与列表顺序一致）
     */
    public NodeChain(List<Node> nodes) {
        this.nodes = nodes;
    }

    /**
     * 按顺序执行所有节点逻辑（串行执行，前一个完成再执行下一个）
     *
     * @param request 请求参数
     * @param context 节点上下文
     * @return 空Mono标识所有节点执行完成
     */
    public Mono<Void> execute(String request, NodeContext context) {
        Mono<Void> chain = Mono.empty();
        for (Node node : nodes) {
            chain = chain.then(node.execute(request, context));
        }
        return chain;
    }
}