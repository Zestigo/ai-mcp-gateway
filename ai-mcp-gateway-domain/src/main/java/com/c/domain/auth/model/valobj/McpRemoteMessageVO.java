package com.c.domain.auth.model.valobj;

import com.c.domain.session.model.valobj.McpSchemaVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 跨节点消息传输对象（领域层值对象）
 * 属于 Domain 层，由 Trigger 层和 Infrastructure 层共同引用
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class McpRemoteMessageVO implements Serializable {
    /** 目标会话ID */
    private String sessionId;
    /** JSON-RPC 协议消息体 */
    private McpSchemaVO.JSONRPCMessage message;
}