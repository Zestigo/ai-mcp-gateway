package com.c.infrastructure.adapter.port;

import com.alibaba.fastjson.JSON;
import com.c.domain.session.adapter.port.SessionPort;
import com.c.domain.session.model.valobj.gateway.McpGatewayProtocolConfigVO;
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
 * 会话端口适配器实现类
 * 验证网关对多协议（GET/POST）动态路由的准确性及资源回收的安全性
 *
 * @author cyh
 * @date 2026/03/25
 */
@Slf4j
@Component
public class SessionPortAdapter implements SessionPort {

    /**
     * 路径参数正则匹配模式：用于匹配 URL 中类似 {userId} 的占位符
     * \\{ 匹配左大括号，([^}]+) 捕获括号内非右大括号的任意字符，} 匹配右大括号
     */
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^}]+)}");

    /** 通用 HTTP 请求网关服务：基于 Retrofit 封装的底层通信组件 */
    @Resource
    private GenericHttpGateway gateway;

    /** Jackson 对象映射器：用于处理数据库中存储的 JSON 字符串配置 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行 MCP 工具调用核心入口
     *
     * @param httpConfig 包含 URL（如 http://localhost:8091/api）、方法（POST/GET）及 Header 的配置对象
     * @param params     前端或上游传入的原始请求参数（通常是 Map 结构）
     * @return 经过下游接口调用后返回的业务数据字符串
     * @throws IOException 网络连接超时、读写中断或 HTTP 协议解析异常
     */
    @Override
    public Object toolCall(McpGatewayProtocolConfigVO.HTTPConfig httpConfig, Object params) throws IOException {
        // 1. 准入校验：判断 params 是否为 Map 类型且不为空。instanceof Map<?, ?> 确保了类型兼容性
        if (!(params instanceof Map<?, ?> arguments) || arguments.isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER);
        }

        // 2. 泛型补偿：Jackson 的 readValue 默认会将 JSON 转为 LinkedHashMap<String, Object>
        // 使用 TypeReference 是为了告诉编译器：我确定这个字符串反序列化后就是 Map<String, Object> 类型，消除警告
        Map<String, Object> headers = objectMapper.readValue(httpConfig.getHttpHeaders(), new TypeReference<>() {
        });

        // 3. 提取参数：由于网关协议约定，params 的 values 中第一个元素即为核心业务参数 Map
        @SuppressWarnings("unchecked") Map<String, Object> firstArgMap = (Map<String, Object>) arguments
                .values()
                .iterator()
                .next();

        // 4. 动态分发：使用 Java 17 增强 switch。它比传统 switch 更简洁，不需要 break，且支持直接返回值
        return switch (httpConfig
                .getHttpMethod()
                .toLowerCase()) {
            case "post" -> executePost(httpConfig.getHttpUrl(), headers, firstArgMap);
            case "get" -> executeGet(httpConfig.getHttpUrl(), headers, firstArgMap);
            default -> throw new AppException(ResponseCode.METHOD_NOT_FOUND);
        };
    }

    /**
     * 执行同步 POST 请求
     *
     * @param url     从数据库加载的目标接口完整地址
     * @param headers 经过解析后的请求头映射表
     * @param bodyMap 需要作为 JSON 发送的业务参数
     * @return 下游接口返回的纯文本或 JSON 字符串
     * @throws IOException 请求执行过程中的 IO 异常
     *                     验证 RequestBody (OkHttp) 的构建是否符合 application/json 标准，并确保 ResponseBody 确定性关闭
     */
    private String executePost(String url, Map<String, Object> headers, Map<String, Object> bodyMap) throws IOException {
        // 1. 序列化：将 Map 转换为标准的 JSON 字符串，并封装为 OkHttp 认可的 RequestBody
        RequestBody requestBody = RequestBody.create(JSON.toJSONString(bodyMap), MediaType.parse("application/json"));

        // 2. 执行调用：gateway.post 返回一个 Retrofit 的 Call 对象，execute() 发起同步阻塞请求
        Response<ResponseBody> response = gateway
                .post(url, headers, requestBody)
                .execute();

        // 3. 资源回收：ResponseBody 实现了 AutoCloseable。在 try() 中声明它，
        // 无论代码正常结束还是抛出异常，Java 都会自动调用 responseBody.close() 释放网络连接
        try (ResponseBody responseBody = response.body()) {
            // 4. 结果判断：只有状态码在 200-299 之间且 Body 不为空时才读取 string
            return (response.isSuccessful() && responseBody != null) ? responseBody.string() : null;
        }
    }

    /**
     * 执行同步 GET 请求并处理路径参数
     *
     * @param url     包含占位符的原始路径，例如：/api/user/{id}/info
     * @param headers 请求头映射表
     * @param argMap  参数表，其中包含对应 {id} 的键值对，以及其他的查询参数
     * @return 下游接口返回的字符串结果
     * @throws IOException 网络异常
     *                     验证正则表达式对 URL 占位符的精准替换，防止已作为路径变量的参数再次出现在 Query 字符串中
     */
    private String executeGet(String url, Map<String, Object> headers, Map<String, Object> argMap) throws IOException {
        // 1. 副本保护：构造一个新的 HashMap，防止后续的 remove 操作破坏原始的参数对象
        Map<String, Object> queryParams = new HashMap<>(argMap);

        // 2. 占位符解析：使用正则表达式寻找 URL 中的 {xxx}
        String finalUrl = url;
        Matcher matcher = PATH_PARAM_PATTERN.matcher(finalUrl);
        while (matcher.find()) {
            // group(1) 拿到的是大括号里面的内容，比如 "id"
            String placeholder = matcher.group(1);
            if (queryParams.containsKey(placeholder)) {
                // 3. 动态替换：将 {id} 替换为参数中对应的实际值，比如 "1001"
                finalUrl = finalUrl.replace("{" + placeholder + "}", String.valueOf(queryParams.get(placeholder)));
                // 4. 参数剔除：既然这个参数已经放在 URL 路径里了，就不应该再作为 ?id=1001 拼接在后面
                queryParams.remove(placeholder);
            }
        }

        // 5. 发起 GET 调用：剩余在 queryParams 里的参数会被 Retrofit 自动拼接成 Query String
        Response<ResponseBody> response = gateway
                .get(finalUrl, headers, queryParams)
                .execute();

        // 6. 安全释放资源并返回结果
        try (ResponseBody responseBody = response.body()) {
            return (response.isSuccessful() && responseBody != null) ? responseBody.string() : null;
        }
    }
}