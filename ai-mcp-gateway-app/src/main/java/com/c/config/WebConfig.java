package com.c.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Web MVC 全局配置类
 * 负责配置WebMvc核心功能，包括异步请求处理、拦截器、资源映射等扩展配置
 *
 * @author cyh
 * @date 2026/03/25
 */
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    /** MVC异步请求专用线程池 */
    @Autowired
    private ThreadPoolTaskExecutor mvcTaskExecutor;

    /**
     * 配置Spring MVC异步请求支持
     * 绑定异步任务执行器并设置全局异步请求超时时间
     *
     * @param configurer 异步支持配置器
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 设置异步请求使用的自定义线程池
        configurer.setTaskExecutor(mvcTaskExecutor);
        // 设置异步请求默认超时时间 60秒
        configurer.setDefaultTimeout(60000);
    }
}