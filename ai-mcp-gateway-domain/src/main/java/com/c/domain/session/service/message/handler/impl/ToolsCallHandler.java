package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * RPC请求处理器-工具调用执行，处理指定工具的调用请求并返回执行结果
 *
 * @author cyh
 * @date 2026/03/20
 */
@Slf4j
@Service("toolsCallHandler")
public class ToolsCallHandler implements IRequestHandler {

    @Override
    public McpSchemaVO.JSONRPCResponse handle(McpSchemaVO.JSONRPCRequest message) {
        return null;
    }

}