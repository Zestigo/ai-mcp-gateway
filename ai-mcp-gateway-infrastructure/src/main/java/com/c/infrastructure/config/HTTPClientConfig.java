package com.c.infrastructure.config;

import com.c.infrastructure.gateway.GenericHttpGateway;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * HTTP客户端配置类
 * 基于OkHttp + Retrofit实现HTTP调用自动化配置，提供统一的网络请求客户端与网关实例
 *
 * @author cyh
 * @date 2026/03/25
 */
@Slf4j
@Configuration
public class HTTPClientConfig {

    /** 服务对外完整访问地址 */
    @Value("${service.full-url}")
    private String serviceFullUrl;

    /**
     * 配置OkHttp客户端实例
     * 包含连接池、超时时间、失败重试等核心网络参数配置
     *
     * @return 配置完成的OkHttpClient对象
     */
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                // 配置连接池：最大空闲连接数10，空闲连接存活时间5分钟
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                // 开启连接失败自动重试
                .retryOnConnectionFailure(true)
                // 连接超时时间：100秒
                .connectTimeout(100, TimeUnit.SECONDS)
                // 读取超时时间：300秒
                .readTimeout(300, TimeUnit.SECONDS)
                // 写入超时时间：300秒
                .writeTimeout(300, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 创建通用HTTP网关代理实例
     * 基于Retrofit构建，注入自定义OkHttpClient，使用Gson完成数据序列化
     *
     * @param okHttpClient 自定义配置的OkHttp客户端
     * @return GenericHttpGateway接口代理实现对象
     */
    @Bean
    public GenericHttpGateway genericHttpGateway(OkHttpClient okHttpClient) {
        String baseUrl = serviceFullUrl;
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        log.info("Retrofit BaseUrl 加载成功: {}", baseUrl);
        Retrofit retrofit = new Retrofit.Builder()
                // 设置基础请求地址
                .baseUrl(baseUrl)
                // 添加Gson转换器，处理JSON数据解析
                .addConverterFactory(GsonConverterFactory.create())
                // 注入自定义配置的OkHttp客户端
                .client(okHttpClient)
                .build();
        // 创建GenericHttpGateway接口的动态代理对象
        return retrofit.create(GenericHttpGateway.class);
    }
}