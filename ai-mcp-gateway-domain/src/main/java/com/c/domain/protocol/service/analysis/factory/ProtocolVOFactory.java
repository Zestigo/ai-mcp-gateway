package com.c.domain.protocol.service.analysis.factory;

import com.alibaba.fastjson2.JSON;
import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 协议对象工厂
 * 统一构建HTTPProtocolVO基础实例，标准化URL拼接、超时时间与默认请求头
 *
 * @author cyh
 * @date 2026/03/28
 */
@Slf4j
public class ProtocolVOFactory {

    /** 默认超时时间，单位毫秒 */
    private static final Integer DEFAULT_TIMEOUT = 30000;

    /**
     * 创建标准HTTP协议配置对象
     *
     * @param baseUrl  基础路径
     * @param endpoint 接口端点
     * @param method   请求方式
     * @return 填充基础配置的HTTPProtocolVO对象
     */
    public static HTTPProtocolVO create(String baseUrl, String endpoint, String method) {
        // 标准化拼接完整请求地址
        String fullUrl = normalizeUrl(baseUrl, endpoint);

        // 初始化通用请求头
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Content-Type", "application/json;charset=UTF-8");
        headerMap.put("Accept", "application/json");
        headerMap.put("X-Gateway-Source", "MCP-Gateway-V2");

        // 构建并返回标准协议VO
        return HTTPProtocolVO
                .builder()
                .httpUrl(fullUrl)
                .httpMethod(method.toUpperCase())
                .httpHeaders(JSON.toJSONString(headerMap))
                .timeout(DEFAULT_TIMEOUT)
                .build();
    }

    /**
     * 标准化URL拼接，处理路径分隔符问题
     *
     * @param baseUrl  基础路径
     * @param endpoint 接口端点
     * @return 标准化后的完整URL
     */
    private static String normalizeUrl(String baseUrl, String endpoint) {
        // 空值安全处理
        if (baseUrl == null) baseUrl = "";
        if (endpoint == null) endpoint = "";

        // 情况1：baseUrl 以 / 结尾 + endpoint 以 / 开头 → 去重一个 /
        if (baseUrl.endsWith("/") && endpoint.startsWith("/")) {
            return baseUrl + endpoint.substring(1);
        }

        // 情况2：两边都不带 /，且都不为空 → 自动补 /
        if (!baseUrl.endsWith("/") && !endpoint.startsWith("/") && !baseUrl.isEmpty() && !endpoint.isEmpty()) {
            return baseUrl + "/" + endpoint;
        }

        // 其他情况直接拼接
        return baseUrl + endpoint;
    }
}