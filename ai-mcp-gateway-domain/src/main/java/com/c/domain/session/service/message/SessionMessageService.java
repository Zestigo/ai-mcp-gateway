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
        return Mono.just(request)
                   // 1. 基础校验：抽离成逻辑算子
                   .handle((req, sink) -> {
                       String method = req.method();
                       if (method == null || method.isBlank()) {
                           sink.error(new AppException("0003", "method 不能为空"));
                           return;
                       }
                       sink.next(req);
                   })
                   .cast(McpSchemaVO.JSONRPCRequest.class)
                   // 2. 路由逻辑：寻找策略
                   .flatMap(req -> {
                       String method = req.method();
                       log.info("流式路由请求 | Method: [{}], ID: [{}]", method, req.id());

                       return Mono
                               .justOrEmpty(SessionMessageHandlerMethodEnum.getByMethod(method))
                               .switchIfEmpty(Mono.error(() -> {
                                   log.error("路由失败：未定义方法 [{}]", method);
                                   return new AppException("0003", "未找到方法枚举定义: " + method);
                               }))
                               .flatMap(strategy -> {
                                   String handlerName = strategy.getHandlerName();
                                   IRequestHandler handler = requestHandlerMap.get(handlerName);
                                   if (handler == null) {
                                       return Mono.error(new AppException("0003", "未找到处理器实例: " + handlerName));
                                   }

                                   log.info("匹配成功 | 处理器: {}", handler
                                           .getClass()
                                           .getSimpleName());

                                   // 3. 执行业务逻辑
                                   // 注意：如果你的 handler.handle 是阻塞的，包在 fromCallable 里
                                   return Mono
                                           .fromCallable(() -> handler.handle(req))
                                           .subscribeOn(Schedulers.boundedElastic())
                                           .flatMap(response -> {
                                               // 4. 判断是否是通知
                                               if (req.isNotification() || method.startsWith("notifications/")) {
                                                   log.info("通知类请求处理完成，流式返回 Empty | method: {}", method);
                                                   return Mono.empty(); // 通知不需要响应，返回空流
                                               }

                                               if (response == null) {
                                                   return Mono.error(new AppException("0003",
                                                           "Handler 返回了空响应: " + handlerName));
                                               }
                                               return Mono.just(response);
                                           });
                               });
                   })
                   // 全局错误日志记录
                   .doOnError(e -> log.error("消息路由流处理异常: {}", e.getMessage()))
                   .subscribeOn(Schedulers.boundedElastic());
    }
}