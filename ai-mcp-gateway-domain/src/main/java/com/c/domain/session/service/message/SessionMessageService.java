package com.c.domain.session.service.message;

import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.enums.SessionMessageHandlerMethodEnum;
import com.c.domain.session.service.message.handler.IRequestHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * 会话消息处理服务实现
 *
 * @author cyh
 * @date 2026/03/24
 */
@Service
@RequiredArgsConstructor
public class SessionMessageService implements ISessionMessageService {

    /** 请求处理器集合 */
    private final Map<String, IRequestHandler> handlers;

    /**
     * 处理JSON-RPC消息
     *
     * @param gatewayId 网关标识
     * @param req       请求消息
     * @return 响应结果
     */
    @Override
    public Mono<McpSchemaVO.JSONRPCResponse> processHandlerMessage(
            String gatewayId,
            McpSchemaVO.JSONRPCRequest req) {

        return Mono
                .fromCallable(() -> {
                    // 根据方法名获取对应枚举
                    var methodEnum = SessionMessageHandlerMethodEnum
                            .getByMethod(req.method())
                            .orElseThrow(() -> new RuntimeException("method not found"));

                    // 获取对应处理器
                    IRequestHandler handler = handlers.get(methodEnum.getHandlerName());

                    // 处理器不存在则抛出异常
                    if (handler == null) {
                        throw new RuntimeException("handler missing");
                    }

                    // 执行处理逻辑
                    return handler.handle(gatewayId, req);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}