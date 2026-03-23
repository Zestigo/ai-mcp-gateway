package com.c.infrastructure.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 线程池配置属性类
 * 功能：绑定配置文件中以"thread.pool.executor.config"为前缀的配置项
 * 作用：将配置文件参数映射为Java对象，便于线程池配置类使用
 *
 * @author cyh
 * @date 2026/03/19
 */
@Data
@ConfigurationProperties(prefix = "thread.pool.executor.config", // 配置项前缀
        ignoreInvalidFields = true // 忽略无效配置字段（避免配置错误导致启动失败）
)
public class ThreadPoolConfigProperties {

    /** 核心线程数：线程池常驻的最小线程数，默认20 */
    private Integer corePoolSize = 20;

    /** 最大线程数：线程池允许创建的最大线程数，默认200 */
    private Integer maxPoolSize = 200;

    /** 空闲线程存活时间：临时线程空闲时的最大存活时间（秒），默认10秒 */
    private Long keepAliveTime = 10L;

    /** 阻塞队列大小：任务队列的最大容量，默认5000 */
    private Integer blockQueueSize = 5000;

    /**
     * 任务拒绝策略：当线程池和队列都满时的处理策略，默认AbortPolicy
     * 可选值说明：
     * - AbortPolicy：丢弃任务并抛出RejectedExecutionException异常（默认）
     * - DiscardPolicy：直接丢弃任务，不抛出异常
     * - DiscardOldestPolicy：删除队列中最早的任务，尝试重新提交当前任务
     * - CallerRunsPolicy：由调用者线程执行被拒绝的任务
     */
    private String policy = "AbortPolicy";

}