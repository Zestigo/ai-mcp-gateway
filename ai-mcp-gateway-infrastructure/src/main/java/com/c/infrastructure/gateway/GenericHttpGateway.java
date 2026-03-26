package com.c.infrastructure.gateway;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;

import java.util.Map;

/**
 * 通用HTTP请求网关接口
 * 基于Retrofit实现，提供标准化的GET/POST请求能力，适配外部接口调用场景
 * 参考文档：<a href="https://bugstack.cn/md/road-map/http.html">HTTP 框架案例</a>
 *
 * @author cyh
 * @date 2026/03/25
 */
public interface GenericHttpGateway {

    /**
     * 发起POST请求
     *
     * @param url     请求地址（动态URL）
     * @param headers 请求头参数集合
     * @param body    请求体数据
     * @return 响应体包装对象
     */
    @POST
    Call<ResponseBody> post(@Url String url, @HeaderMap Map<String, Object> headers, @Body RequestBody body);

    /**
     * 发起GET请求
     *
     * @param url         请求地址（动态URL）
     * @param headers     请求头参数集合
     * @param queryParams URL查询参数集合
     * @return 响应体包装对象
     */
    @GET
    Call<ResponseBody> get(@Url String url, @HeaderMap Map<String, Object> headers,
                           @QueryMap Map<String, Object> queryParams);

}