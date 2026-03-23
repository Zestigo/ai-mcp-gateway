package com.c.domain.session.service.message;

import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.enums.SessionMessageHandlerMethodEnum;
import com.c.domain.session.service.ISessionMessageService;
import com.c.domain.session.service.message.handler.IRequestHandler;
import com.c.types.exception.AppException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * 会话消息核心服务：路由JSON-RPC请求到对应处理器执行
 *
 * @author cyh
 * @date 2026/03/23
 */
@Slf4j
@Service
public class SessionMessageService implements ISessionMessageService {

    /** Spring自动注入IRequestHandler实现类：Key=Bean名称，Value=处理器实例 */
    @Resource
    private Map<String, IRequestHandler> requestHandlerMap;

    /**
     * 处理JSON-RPC请求，路由到对应处理器
     *
     * @param request JSON-RPC请求对象
     * @return JSON-RPC响应对象（响应式）
     */
    @Override
    public Mono<McpSchemaVO.JSONRPCResponse> processHandlerMessage(McpSchemaVO.JSONRPCRequest request) {
        return Mono
                .fromCallable(() -> {
                    String method = request.method();
                    // 入参校验：method不能为空
                    if (method == null || method.isBlank()) {
                        throw new AppException("0003", "method 不能为空");
                    }

                    log.info("路由请求 | Method: [{}], ID: [{}]", method, request.id());

                    // 匹配方法枚举
                    SessionMessageHandlerMethodEnum strategy = SessionMessageHandlerMethodEnum
                            .getByMethod(method)
                            .orElseThrow(() -> {
                                log.error("路由失败：未定义方法 [{}]", method);
                                return new AppException("0003", "未找到方法枚举定义: " + method);
                            });

                    // 查找处理器实例
                    String handlerName = strategy.getHandlerName();
                    IRequestHandler handler = requestHandlerMap.get(handlerName);
                    if (handler == null) {
                        log.error("路由失败：未找到处理器Bean [{}]", handlerName);
                        throw new AppException("0003", "未找到处理器实例: " + handlerName);
                    }

                    // 执行处理器逻辑
                    log.info("匹配成功 | 处理器: {}", handler
                            .getClass()
                            .getSimpleName());
                    McpSchemaVO.JSONRPCResponse response = handler.handle(request);

                    // 通知类请求：不返回响应
                    if (request.isNotification() || method.startsWith("notifications/")) {
                        log.info("通知类请求处理完成 | method: {}", method);
                        return null;
                    }

                    // 响应校验：非通知类请求响应不能为空
                    if (response == null) {
                        throw new AppException("0003", "Handler 返回了 null: " + handlerName);
                    }

                    return response;
                })
                .subscribeOn(Schedulers.boundedElastic()); // 切换至弹性线程池，避免阻塞IO线程
    }
}