package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * RPC请求处理器-工具列表查询，返回服务器支持的所有工具列表
 *
 * @author cyh
 * @date 2026/03/20
 */
@Slf4j
@Service("toolsListHandler")
public class ToolsListHandler implements IRequestHandler {

    @Override
    public McpSchemaVO.JSONRPCResponse handle(McpSchemaVO.JSONRPCRequest message) {
        return null;
    }

}