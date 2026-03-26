package com.c.infrastructure.adapter.port;

import com.c.domain.session.adapter.port.SessionPort;
import com.c.domain.session.model.valobj.gateway.McpToolProtocolConfigVO;
import com.c.infrastructure.gateway.GenericHttpGateway;
import com.c.types.enums.ResponseCode;
import com.c.types.exception.AppException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Component;
import retrofit2.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 会话端口适配器
 * 实现SessionPort接口，提供HTTP协议工具调用能力
 * 支持GET/POST请求动态分发、路径参数解析、统一响应处理
 */
@Slf4j
@Component
public class SessionPortAdapter implements SessionPort {

    /** URL路径参数正则表达式 */
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^}]+)}");

    /** 通用HTTP网关调用器 */
    @Resource
    private GenericHttpGateway gateway;

    /** JSON序列化工具 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行MCP工具HTTP远程调用
     *
     * @param httpConfig HTTP请求配置信息
     * @param params     工具调用参数
     * @return 接口响应结果
     * @throws IOException 请求过程中发生IO异常
     */
    @Override
    public Object toolCall(McpToolProtocolConfigVO.HTTPConfig httpConfig, Object params) throws IOException {
        // 校验参数必须为Map结构
        if (!(params instanceof Map<?, ?> arguments)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER, "参数必须为 Map 结构");
        }

        // 解析请求头配置
        Map<String, Object> headers = objectMapper.readValue(httpConfig.getHttpHeaders(), new TypeReference<>() {
        });

        // 转换为通用参数Map
        @SuppressWarnings("unchecked") Map<String, Object> argMap = (Map<String, Object>) arguments;

        log.info("MCP_HTTP_INVOKE | method={} | url={}", httpConfig.getHttpMethod(), httpConfig.getHttpUrl());

        // 根据请求方法分发处理
        return switch (httpConfig
                .getHttpMethod()
                .toUpperCase()) {
            case "POST" -> executePost(httpConfig.getHttpUrl(), headers, argMap);
            case "GET" -> executeGet(httpConfig.getHttpUrl(), headers, argMap);
            default ->
                    throw new AppException(ResponseCode.METHOD_NOT_FOUND, "不支持的协议类型: " + httpConfig.getHttpMethod());
        };
    }

    /**
     * 执行POST请求
     *
     * @param url     请求地址
     * @param headers 请求头
     * @param bodyMap 请求体参数
     * @return 响应字符串
     * @throws IOException IO异常
     */
    private String executePost(String url, Map<String, Object> headers, Map<String, Object> bodyMap) throws IOException {
        // 将参数序列化为JSON请求体
        String jsonBody = objectMapper.writeValueAsString(bodyMap);
        RequestBody requestBody = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));

        // 发送POST请求
        Response<ResponseBody> response = gateway
                .post(url, headers, requestBody)
                .execute();

        // 统一处理响应
        return handleResponse(response);
    }

    /**
     * 执行GET请求
     *
     * @param url     请求地址
     * @param headers 请求头
     * @param argMap  请求参数
     * @return 响应字符串
     * @throws IOException IO异常
     */
    private String executeGet(String url, Map<String, Object> headers, Map<String, Object> argMap) throws IOException {
        Map<String, Object> queryParams = new HashMap<>(argMap);
        String finalUrl = url;

        // 替换路径中的占位符参数
        Matcher matcher = PATH_PARAM_PATTERN.matcher(finalUrl);
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            if (queryParams.containsKey(placeholder)) {
                finalUrl = finalUrl.replace("{" + placeholder + "}", String.valueOf(queryParams.get(placeholder)));
                queryParams.remove(placeholder);
            }
        }

        // 发送GET请求
        Response<ResponseBody> response = gateway
                .get(finalUrl, headers, queryParams)
                .execute();
        return handleResponse(response);
    }

    /**
     * 统一处理HTTP响应结果
     *
     * @param response 原始响应对象
     * @return 处理后的响应内容
     * @throws IOException IO异常
     */
    private String handleResponse(Response<ResponseBody> response) throws IOException {
        if (response.isSuccessful()) {
            try (ResponseBody body = response.body()) {
                return body != null ? body.string() : "";
            }
        } else {
            // 处理调用失败情况
            try (ResponseBody errorBody = response.errorBody()) {
                String errorMsg = errorBody != null ? errorBody.string() : "Unknown Error";
                log.error("下游接口调用失败 | code={} | msg={}", response.code(), errorMsg);
                return "Error from downstream: " + errorMsg;
            }
        }
    }
}