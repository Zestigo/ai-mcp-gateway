package com.c.cases.mcp.api.service;

import com.c.domain.session.model.entity.HandleMessageCommandEntity;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

/**
 * MCP 消息处理服务接口
 * 职责：处理 JSON-RPC 指令回执，以及向已存在的 SSE 通道异步推送数据
 */
public interface McpMessageService {

    /**
     * 处理前端发送的 MCP 指令消息 (Request-Response 模型)
     */
    Mono<ResponseEntity<Void>> handleMessage(HandleMessageCommandEntity commandEntity);

    /**
     * 后端主动向指定会话推送消息 (Async Push 模型)
     * @param sessionId 会话ID
     * @param message 消息对象（会自动转 JSON）
     */
    Mono<Void> pushMessage(String sessionId, Object message);
}