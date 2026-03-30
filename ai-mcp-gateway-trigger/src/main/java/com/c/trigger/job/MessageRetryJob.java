package com.c.trigger.job;

import com.c.domain.protocol.service.ProtocolStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 本地消息表补偿定时任务
 * 定时拉取待重试的协议刷新消息，保证分布式事务最终一致性
 * 遵循分层架构：仅依赖领域服务，不直接操作基础设施层
 *
 * @author cyh
 * @date 2026/03/29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRetryJob {

    /** 协议存储领域服务 封装消息重试、状态流转、消息发送的核心业务逻辑 */
    private final ProtocolStorage protocolStorage;

    /**
     * 定时执行消息补偿任务
     * 定时规则：每30秒执行一次
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void exec() {
        // 任务开始日志，便于监控与问题排查
        log.info("【触发器】开始执行本地消息表补偿任务...");
        try {
            // 委托领域服务执行重试逻辑：任务触发器只负责调度，不承载业务逻辑
            protocolStorage.retrySendMessages();
            // 任务正常完成日志
            log.info("【触发器】本地消息表补偿任务处理完成");
        } catch (Exception e) {
            // 捕获全局异常，避免单次任务异常导致后续定时任务中断
            log.error("【触发器】执行补偿任务出现异常", e);
        }
    }
}