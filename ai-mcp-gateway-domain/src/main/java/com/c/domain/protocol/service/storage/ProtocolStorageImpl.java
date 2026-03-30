package com.c.domain.protocol.service.storage;

import com.c.domain.protocol.adapter.repository.EventPublisher;
import com.c.domain.protocol.adapter.repository.ProtocolRepository;
import com.c.domain.protocol.model.entity.StorageCommandEntity;
import com.c.domain.protocol.model.valobj.ProtocolRefreshMessage;
import com.c.domain.protocol.model.valobj.enums.EventActionEnum;
import com.c.domain.protocol.service.ProtocolStorage;
import com.c.types.enums.MessageStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 协议存储领域服务实现
 * 遵循DDD领域层规范，负责协议配置核心业务流程编排
 * 核心模式：本地消息表（发件箱模式）保证分布式事务最终一致性
 *
 * @author cyh
 * @date 2026/03/30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProtocolStorageImpl implements ProtocolStorage {

    /** 协议仓储接口：领域层定义，基础设施层实现，负责数据持久化 */
    private final ProtocolRepository protocolRepository;

    /** 事件发布器：负责协议刷新事件推送（MQ/事件总线） */
    private final EventPublisher eventPublisher;

    /**
     * 协议核心存储流程
     * 全流程事务保证：协议数据 + 本地消息表原子性入库
     * 1. 持久化协议配置
     * 2. 记录本地消息（发件箱）
     * 3. 事务提交后推送刷新事件
     *
     * @param commandEntity 协议存储命令实体
     * @return 协议ID集合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> doStorage(StorageCommandEntity commandEntity) {
        // 空安全打印待处理协议数量，便于日志追踪与问题排查
        log.info("开始执行协议持久化，协议数量: {}", Optional
                .ofNullable(commandEntity.getHttpProtocolVOS())
                .map(List::size)
                .orElse(0));

        // 1. 调用仓储层批量保存协议
        // 分布式锁、幂等性、数据库操作全部在基础设施层实现，领域层只关注业务逻辑
        List<Long> protocolIds = protocolRepository.batchSaveProtocols(commandEntity.getHttpProtocolVOS());

        // 无有效协议ID时直接终止流程，避免无效消息生成
        if (protocolIds == null || protocolIds.isEmpty()) {
            log.warn("持久化未产生有效ID，流程中止");
            return Collections.emptyList();
        }

        // 2. 构建协议刷新消息，用于通知网关节点刷新配置
        ProtocolRefreshMessage message = ProtocolRefreshMessage
                .builder()
                .eventType(EventActionEnum.REFRESH.getCode())
                .protocolIds(protocolIds)
                .timestamp(System.currentTimeMillis())
                .build();

        // 本地消息表入库：与协议数据在同一个事务，保证原子性
        String messageId = protocolRepository.saveMessageLog(message);

        // 3. 注册事务钩子：确保数据库事务提交成功后再推送消息
        // 避免消息发送成功但事务回滚，导致网关拉取不到数据
        registerPostCommitPush(messageId, message);

        return protocolIds;
    }

    /**
     * 消息补偿重试入口
     * 由定时任务定期调用，扫描失败/待重试消息进行补偿推送
     * 保证分布式事务最终一致性
     */
    @Override
    public void retrySendMessages() {
        // 批量查询待重试消息，限制50条防止内存溢出
        List<ProtocolRefreshMessage> waitMessages = protocolRepository.queryWaitMessages(50);
        if (waitMessages == null || waitMessages.isEmpty()) return;

        // 逐条处理待补偿消息
        for (ProtocolRefreshMessage message : waitMessages) {
            try {
                // 重新发布协议刷新事件
                eventPublisher.publishProtocolRefresh(message);
                // 推送成功，更新消息状态为成功
                protocolRepository.updateMessageLogStatus(message.getMessageId(), MessageStatusEnum.SUCCESS);
            } catch (Exception e) {
                // 推送失败，记录日志并执行重试策略
                log.error("补偿重试失败，MessageId: {}", message.getMessageId(), e);
                // 失败处理：更新重试次数与下次重试时间（指数退避）
                handleRetryFailure(message);
            }
        }
    }

    /**
     * 注册事务提交后推送钩子
     * Spring事务同步机制：确保业务数据完全提交后再发送消息
     *
     * @param messageId 消息ID
     * @param message   协议刷新消息
     */
    private void registerPostCommitPush(String messageId, ProtocolRefreshMessage message) {
        // 判断当前是否存在活跃事务
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // 注册事务同步回调，事务提交后执行
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("事务提交成功，触发实时刷新推送，MessageId: {}", messageId);
                    // 执行消息推送
                    publishRefreshEvent(messageId, message);
                }
            });
        } else {
            // 无事务环境，直接推送消息
            publishRefreshEvent(messageId, message);
        }
    }

    /**
     * 协议刷新事件推送
     * 统一封装首次推送逻辑，异常捕获并更新消息状态
     * 推送失败不影响主流程，由定时任务负责补偿
     *
     * @param messageId 消息ID
     * @param message   协议刷新消息
     */
    private void publishRefreshEvent(String messageId, ProtocolRefreshMessage message) {
        try {
            // 发布刷新事件到消息队列/事件总线
            eventPublisher.publishProtocolRefresh(message);
            // 推送成功，更新消息状态
            protocolRepository.updateMessageLogStatus(messageId, MessageStatusEnum.SUCCESS);
        } catch (Exception e) {
            // 推送异常，标记为失败，等待定时任务重试
            log.warn("首次推送异常，已记录FAIL状态等待Job补偿: {}", messageId);
            protocolRepository.updateMessageLogStatus(messageId, MessageStatusEnum.FAIL);
        }
    }

    /**
     * 消息重试失败处理
     * 采用指数退避算法：重试间隔随次数指数增长，避免频繁重试压垮服务
     * 超过最大重试次数标记为超限，转人工介入
     *
     * @param message 待处理消息
     */
    private void handleRetryFailure(ProtocolRefreshMessage message) {
        // 空安全获取当前重试次数并自增
        int currentRetry = Optional
                .ofNullable(message.getRetryCount())
                .orElse(0) + 1;

        // 重试次数达到上限（3次），标记为最终失败
        if (currentRetry >= 3) {
            log.error("消息重试达上限，转人工处理: {}", message.getMessageId());
            protocolRepository.updateMessageLogStatus(message.getMessageId(), MessageStatusEnum.OVER_LIMIT);
        } else {
            // 指数退避计算：2^n * 1分钟
            // 第1次：2分钟，第2次：4分钟，第3次：8分钟
            long delay = (long) Math.pow(2, currentRetry) * 60 * 1000;
            Date nextTime = new Date(System.currentTimeMillis() + delay);
            // 更新重试次数与下次重试时间
            protocolRepository.updateRetryInfo(message.getMessageId(), currentRetry, nextTime);
        }
    }
}