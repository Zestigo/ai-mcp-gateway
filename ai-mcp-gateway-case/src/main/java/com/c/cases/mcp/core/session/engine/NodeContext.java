package com.c.cases.mcp.core.session.engine;

import com.c.domain.session.model.valobj.SessionConfigVO;
import lombok.Data;

/**
 * MCP会话节点上下文：存储节点执行所需的会话配置信息
 *
 * @author cyh
 * @date 2026/03/19
 */
@Data
public class NodeContext {

    /** 会话配置值对象 */
    private SessionConfigVO session;

}