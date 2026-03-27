package com.c.domain.session.model.entity;

import com.c.domain.session.model.valobj.McpSchemaVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息处理命令实体
 *
 * @author cyh
 * @date 2026/03/27
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HandleMessageCommandEntity {

    /** 网关ID */
    private String gatewayId;

    /** 会话ID */
    private String sessionId;

    /** JSON-RPC消息 */
    private McpSchemaVO.JSONRPCMessage jsonrpcMessage;

    /**
     * 构造方法
     *
     * @param gatewayId   网关ID
     * @param sessionId   会话ID
     * @param messageBody 消息体
     * @throws Exception 解析异常
     */
    public HandleMessageCommandEntity(String gatewayId, String sessionId, String messageBody) throws Exception {
        this.gatewayId = gatewayId;
        this.sessionId = sessionId;
        this.jsonrpcMessage = McpSchemaVO.deserializeJsonRpcMessage(messageBody);
    }

}