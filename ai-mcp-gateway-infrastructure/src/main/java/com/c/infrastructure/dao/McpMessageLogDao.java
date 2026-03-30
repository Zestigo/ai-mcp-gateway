package com.c.infrastructure.dao;

import com.c.infrastructure.dao.po.McpMessageLogPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 本地消息日志数据访问对象
 * 职责：支撑分布式事务最终一致性，处理消息的存取、状态变更与重试补偿
 *
 * @author cyh
 * @date 2026/03/29
 */
@Mapper
public interface McpMessageLogDao {

    /**
     * 插入新消息记录
     * 初始状态通常为 WAIT
     *
     * @param po 消息持久化对象
     * @return 影响行数
     */
    int insert(McpMessageLogPO po);

    /**
     * 更新消息状态
     * 用于首次发送成功或重试次数达上限后的状态变更
     *
     * @param messageId 消息唯一标识
     * @param status    目标状态 (SUCCESS/FAIL/OVER_LIMIT)
     * @return 影响行数
     */
    int updateStatus(@Param("messageId") String messageId, @Param("status") String status);

    /**
     * 更新重试信息
     * 当投递失败时，累加重试次数并设置下一次执行时间
     *
     * @param messageId     消息唯一标识
     * @param retryCount    已重试次数
     * @param nextRetryTime 下一次允许重试的时间点
     * @return 影响行数
     */
    int updateRetryInfo(@Param("messageId") String messageId, @Param("retryCount") Integer retryCount, @Param(
            "nextRetryTime") Date nextRetryTime);

    /**
     * 捞取待处理/需补偿的消息
     * 逻辑：(status = 'WAIT' OR status = 'FAIL') AND next_retry_time <= NOW()
     * 且重试次数未达上限
     *
     * @param limit 每次捞取的最大条数，防止大事务和内存溢出
     * @return 待重试的消息集合
     */
    List<McpMessageLogPO> queryWaitMessages(@Param("limit") Integer limit);

    /**
     * 根据 ID 查询消息详情（主要用于对账或排查）
     *
     * @param messageId 消息唯一标识
     * @return 消息记录
     */
    McpMessageLogPO queryByMessageId(@Param("messageId") String messageId);

    /**
     * 清理已成功的历史消息（建议在运维脚本中使用，保持表数据量可控）
     *
     * @param beforeTime 清理该时间点之前的成功消息
     * @return 影响行数
     */
    int deleteExpiredSuccessMessages(@Param("beforeTime") Date beforeTime);
}