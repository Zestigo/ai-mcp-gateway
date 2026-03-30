package com.c.domain.protocol.adapter.repository;

import com.c.domain.protocol.model.valobj.ProtocolRefreshMessage;
import com.c.domain.protocol.model.valobj.enums.ProtocolStatusEnum;
import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;
import com.c.types.enums.MessageStatusEnum;

import java.util.Date;
import java.util.List;

/**
 * 协议配置仓储接口
 * 领域层定义的数据访问契约，用于协议配置持久化、查询、状态管理
 * 基于本地消息表实现分布式事务最终一致性
 */
public interface ProtocolRepository {

    /* ========================== 协议核心业务操作 ========================== */

    /**
     * 保存或更新协议配置，保证接口幂等性
     * 实现类需通过 URL + Method 保证唯一性，并结合事务保证数据一致性
     *
     * @param protocolVO 协议配置视图对象
     * @return 生成/更新后的协议ID
     */
    Long saveOrUpdateProtocol(HTTPProtocolVO protocolVO);

    /**
     * 批量保存协议配置
     * 内部会循环逐条处理，保证并发安全
     *
     * @param protocolVOS 协议配置对象集合
     * @return 协议ID集合
     */
    List<Long> batchSaveProtocols(List<HTTPProtocolVO> protocolVOS);

    /**
     * 根据协议ID查询完整协议详情
     * 包含主表信息与字段映射规则
     *
     * @param protocolId 协议ID
     * @return 完整协议视图对象，无数据返回null
     */
    HTTPProtocolVO queryProtocolDetail(Long protocolId);

    /**
     * 根据请求URL + 请求方法精确查询协议
     * 用于网关请求匹配
     *
     * @param url    请求路径
     * @param method 请求方法
     * @return 协议配置对象，无数据返回null
     */
    HTTPProtocolVO queryByUrl(String url, String method);

    /**
     * 更新协议状态（启用/禁用）
     *
     * @param protocolId 协议ID
     * @param status     目标状态枚举
     */
    void updateStatus(Long protocolId, ProtocolStatusEnum status);

    /**
     * 分页模糊查询协议列表
     *
     * @param urlKeyword URL模糊关键词
     * @param page       页码
     * @param size       每页条数
     * @return 协议对象集合
     */
    List<HTTPProtocolVO> queryProtocolPage(String urlKeyword, Integer page, Integer size);

    /**
     * 查询所有处于启用状态的协议ID
     * 用于网关全量刷新配置
     *
     * @return 活跃协议ID集合
     */
    List<Long> queryAllActiveProtocolIds();

    /* ========================== 本地消息表操作（支撑最终一致性） ========================== */

    /**
     * 保存协议刷新消息到本地消息表
     * 用于分布式事务消息补偿，保证消息不丢失
     *
     * @param message 协议刷新消息对象
     * @return 生成的消息唯一ID
     */
    String saveMessageLog(ProtocolRefreshMessage message);

    /**
     * 更新消息状态
     *
     * @param messageId 消息ID
     * @param status    消息状态枚举
     */
    void updateMessageLogStatus(String messageId, MessageStatusEnum status);

    /**
     * 更新消息重试信息
     *
     * @param messageId     消息ID
     * @param retryCount    当前重试次数
     * @param nextRetryTime 下次重试时间
     */
    void updateRetryInfo(String messageId, Integer retryCount, Date nextRetryTime);

    /**
     * 查询待处理/待重试消息
     * 满足：状态为待发送/发送失败 且 已到重试时间
     *
     * @param limit 最大查询条数，防止内存溢出
     * @return 待重试消息集合
     */
    List<ProtocolRefreshMessage> queryWaitMessages(Integer limit);
}