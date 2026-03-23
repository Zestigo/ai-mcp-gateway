package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 工具列表处理器：处理tools/list指令
 *
 * @author cyh
 * @date 2026/03/23
 */
@Slf4j
@Service("toolsListHandler")
public class ToolsListHandler implements IRequestHandler {

    /**
     * 处理工具列表查询请求
     *
     * @param message JSON-RPC请求对象
     * @return JSON-RPC响应对象（包含空工具列表）
     */
    @Override
    public McpSchemaVO.JSONRPCResponse handle(McpSchemaVO.JSONRPCRequest message) {
        log.info("处理工具列表查询 | method: tools/list");

        // TODO: 按MCP协议返回包含tools数组的结果
        Map<String, Object> result = new HashMap<>();
        result.put("tools", new ArrayList<>());

        return new McpSchemaVO.JSONRPCResponse("2.0", message.id(), result, null);
    }
}