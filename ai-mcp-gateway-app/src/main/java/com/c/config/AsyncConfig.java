package com.c.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置类
 * 定义Spring MVC异步处理、后台异步任务执行所需的线程池参数与策略
 *
 * @author cyh
 * @date 2026/03/25
 */
@Configuration
public class AsyncConfig implements WebMvcConfigurer {

    /**
     * 注册MVC异步请求专用线程池Bean
     *
     * @return 配置完成的线程池任务执行器
     */
    @Bean("mvcTaskExecutor")
    public ThreadPoolTaskExecutor mvcTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：线程池长期维持的最小线程数
        executor.setCorePoolSize(10);
        // 最大线程数：线程池能创建的最大线程数量
        executor.setMaxPoolSize(50);
        // 队列容量：缓冲队列的最大等待任务数
        executor.setQueueCapacity(200);
        // 线程名称前缀：方便日志追踪与问题排查
        executor.setThreadNamePrefix("mvc-async-");
        // 拒绝策略：队列满时由调用线程直接执行任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 初始化线程池
        executor.initialize();
        return executor;
    }
}