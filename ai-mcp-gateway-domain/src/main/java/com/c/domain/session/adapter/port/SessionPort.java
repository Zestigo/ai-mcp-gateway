package com.c.domain.session.adapter.port;

import com.c.domain.session.model.valobj.gateway.McpGatewayProtocolConfigVO;

import java.io.IOException;

/**
 * 会话服务端口接口
 * 定义会话层核心工具调用能力
 *
 * @author cyh
 * @date 2026/03/25
 */
public interface SessionPort {

    /**
     * 工具调用统一入口方法
     *
     * @param httpConfig HTTP请求配置参数
     * @param params     业务请求参数
     * @return 调用返回结果
     * @throws IOException IO调用异常
     */
    Object toolCall(McpGatewayProtocolConfigVO.HTTPConfig httpConfig, Object params) throws IOException;

}