package com.c.config;

import com.c.config.properties.ThreadPoolConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.*;

/**
 * 自定义线程池配置类
 * 功能：根据配置文件参数创建全局统一的线程池，支持异步任务执行
 * 特性：优先级加载自定义配置，缺失时使用默认值；仅在无默认ThreadPoolExecutor Bean时创建
 *
 * @author cyh
 * @date 2026/03/19
 */
@Slf4j
@EnableAsync // 开启Spring异步任务支持
@Configuration
@EnableConfigurationProperties(ThreadPoolConfigProperties.class) // 启用线程池配置属性类
public class ThreadPoolConfig {

    /**
     * 创建自定义ThreadPoolExecutor Bean
     *
     * @param properties 线程池配置属性（自动注入配置文件中的参数）
     * @return 配置完成的线程池实例
     * @throws ClassNotFoundException 类加载异常（理论上不会触发）
     * @throws InstantiationException 实例化异常（理论上不会触发）
     * @throws IllegalAccessException 访问权限异常（理论上不会触发）
     */
    @Bean
    @ConditionalOnMissingBean(ThreadPoolExecutor.class) // 避免重复创建线程池Bean
    public ThreadPoolExecutor threadPoolExecutor(ThreadPoolConfigProperties properties) throws ClassNotFoundException
            , InstantiationException, IllegalAccessException {
        // 1. 根据配置的策略名称实例化拒绝策略
        RejectedExecutionHandler handler;
        switch (properties.getPolicy()) {
            case "AbortPolicy":
                // 丢弃任务并抛出RejectedExecutionException异常（默认策略）
                handler = new ThreadPoolExecutor.AbortPolicy();
                break;
            case "DiscardPolicy":
                // 直接丢弃任务，不抛出异常（静默失败）
                handler = new ThreadPoolExecutor.DiscardPolicy();
                break;
            case "DiscardOldestPolicy":
                // 丢弃队列中最旧的任务，再尝试提交当前任务
                handler = new ThreadPoolExecutor.DiscardOldestPolicy();
                break;
            case "CallerRunsPolicy":
                // 由调用者线程（如主线程）执行被拒绝的任务（降级策略）
                handler = new ThreadPoolExecutor.CallerRunsPolicy();
                break;
            default:
                // 默认使用AbortPolicy，保证异常可感知
                log.warn("线程池拒绝策略配置异常：{}，已默认使用AbortPolicy", properties.getPolicy());
                handler = new ThreadPoolExecutor.AbortPolicy();
                break;
        }

        // 2. 构建并返回线程池实例
        return new ThreadPoolExecutor(properties.getCorePoolSize(), // 核心线程数（常驻线程数）
                properties.getMaxPoolSize(),   // 最大线程数（核心+临时线程数上限）
                properties.getKeepAliveTime(), // 临时线程空闲存活时间
                TimeUnit.SECONDS,              // 时间单位（秒）
                new LinkedBlockingQueue<>(properties.getBlockQueueSize()), // 任务阻塞队列（有界）
                Executors.defaultThreadFactory(), // 默认线程工厂（创建线程）
                handler // 任务拒绝策略（队列满+线程数达上限时触发）
        );
    }

}