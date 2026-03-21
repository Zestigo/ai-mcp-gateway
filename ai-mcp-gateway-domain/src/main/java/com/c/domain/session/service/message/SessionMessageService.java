package com.c.domain.session.service.message;

import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.enums.SessionMessageHandlerMethodEnum;
import com.c.domain.session.service.ISessionMessageService;
import com.c.domain.session.service.message.handler.IRequestHandler;
import com.c.types.exception.AppException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.c.types.enums.ResponseCode.METHOD_NOT_FOUND;

/**
 * 会话消息核心服务，负责路由 JSON-RPC 请求到对应处理器执行
 */
@Slf4j
@Service
public class SessionMessageService implements ISessionMessageService {

    /**
     * Spring 会自动将所有 IRequestHandler 的实现类注入到 Map 中
     * Key 为 Bean 的名称（默认首字母小写），Value 为实例
     */
    @Resource
    private Map<String, IRequestHandler> requestHandlerMap;

    @Override
    public McpSchemaVO.JSONRPCResponse processHandlerMessage(McpSchemaVO.JSONRPCRequest request) {
        String method = request.method();
        log.info("收到 JSON-RPC 请求，Method: [{}]", method);

        // 1. 链式解析：查找枚举 -> 获取 Handler 名称 -> 从容器 Map 匹配 Bean -> 执行
        return SessionMessageHandlerMethodEnum
                .getByMethod(method)
                .map(SessionMessageHandlerMethodEnum::getHandlerName)
                .map(handlerName -> {
                    IRequestHandler handler = requestHandlerMap.get(handlerName);
                    if (handler == null) {
                        log.warn("警告：匹配到枚举但未找到对应的处理器 Bean: [{}]", handlerName);
                    }
                    return handler;
                })
                .map(handler -> handler.handle(request))
                // 2. 统一兜底：如果上述任何环节返回 Optional.empty()，则抛出异常
                .orElseThrow(() -> {
                    log.error("请求处理失败：方法 [{}] 未定义或对应的 Handler 缺失", method);
                    return new AppException(METHOD_NOT_FOUND);
                });
    }
}