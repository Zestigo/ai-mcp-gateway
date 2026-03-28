package com.c.domain.session.service.message.handler.impl;

import com.c.domain.session.adapter.port.SessionPort;
import com.c.domain.session.adapter.repository.GatewayRepository;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.model.valobj.gateway.McpToolProtocolConfigVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import com.c.types.enums.ResponseCode;
import com.c.types.exception.AppException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

import static com.c.domain.session.model.valobj.McpSchemaVO.ErrorCodes.INTERNAL_ERROR;

/**
 * MCP工具调用请求处理器
 * 处理tools/call类型请求，完成参数解析、协议配置加载、远程接口调用与响应封装
 *
 * @author cyh
 * @date 2026/03/26
 */
@Slf4j
@Service("toolsCallHandler")
@RequiredArgsConstructor
public class ToolsCallHandler implements IRequestHandler {

    /** 网关配置仓储 */
    private final GatewayRepository gatewayRepository;

    /** 会话服务端口，执行实际接口调用 */
    private final SessionPort sessionPort;

    /**
     * 处理MCP工具调用请求
     *
     * @param gatewayId 网关唯一标识
     * @param message   JSON-RPC请求消息
     * @return 工具调用响应结果流
     */
    @Override
    public Flux<McpSchemaVO.JSONRPCResponse> handle(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        // 校验消息类型是否为合法请求
        if (!(message instanceof McpSchemaVO.JSONRPCRequest req)) {
            return Flux.error(new AppException(ResponseCode.ILLEGAL_PARAMETER));
        }

        return Mono.fromCallable(() -> {
                       // 解析请求参数，获取工具名称与调用参数
                       McpSchemaVO.CallToolRequest callToolRequest = McpSchemaVO.convert(req.params(),
                               new TypeReference<>() {
                               });

                       // 校验工具名称非空
                       if (callToolRequest == null || callToolRequest.name() == null) {
                           throw new AppException(ResponseCode.ILLEGAL_PARAMETER, "工具名称不能为空");
                       }

                       // 根据网关ID与工具名称查询协议配置
                       McpToolProtocolConfigVO protocolConfig =
                               gatewayRepository.queryMcpGatewayProtocolConfig(gatewayId, callToolRequest.name());

                       // 校验协议配置是否存在
                       if (protocolConfig == null || protocolConfig.getHttpConfig() == null) {
                           throw new AppException(ResponseCode.DATA_NOT_FOUND, "未找到工具协议配置: " + callToolRequest.name());
                       }

                       log.info("MCP_CALL_START | gatewayId={} | toolName={}", gatewayId, callToolRequest.name());

                       // 调用会话端口执行远程接口请求
                       return sessionPort.toolCall(protocolConfig.getHttpConfig(), callToolRequest.arguments());
                   })
                   // 数据库与网络IO操作切换至弹性线程池，避免阻塞反应器线程
                   .subscribeOn(Schedulers.boundedElastic())
                   .map(result -> {
                       // 按照MCP协议规范封装成功响应内容
                       Map<String, Object> content = Map.of("type", "text", "text", String.valueOf(result));
                       Map<String, Object> payload = Map.of("content", List.of(content), "isError", false);
                       return McpSchemaVO.JSONRPCResponse.ofSuccess(req.id(), payload);
                   })
                   .onErrorResume(e -> {
                       // 统一异常处理，记录日志并返回标准错误响应
                       log.error("MCP_CALL_FAILED | gatewayId={} | error={}", gatewayId, e.getMessage(), e);
                       int errorCode = (e instanceof AppException ae) ? safeParseInt(ae.getCode(), INTERNAL_ERROR) : INTERNAL_ERROR;
                       return Mono.just(McpSchemaVO.JSONRPCResponse.ofError(req.id(), errorCode, e.getMessage(), null));
                   })
                   // 转换为Flux类型以适配接口返回值
                   .flux();
    }

    /**
     * 安全解析字符串类型错误码
     *
     * @param code        错误码字符串
     * @param defaultCode 默认错误码
     * @return 解析后的整型错误码
     */
    private int safeParseInt(String code, int defaultCode) {
        try {
            return Integer.parseInt(code);
        } catch (Exception e) {
            return defaultCode;
        }
    }
}