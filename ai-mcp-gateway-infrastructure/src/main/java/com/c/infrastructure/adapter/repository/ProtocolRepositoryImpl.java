package com.c.infrastructure.adapter.repository;

import com.alibaba.fastjson.JSON;
import com.c.domain.protocol.adapter.repository.ProtocolRepository;
import com.c.domain.protocol.model.valobj.ProtocolRefreshMessage;
import com.c.domain.protocol.model.valobj.enums.ProtocolStatusEnum;
import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;
import com.c.infrastructure.dao.McpMessageLogDao;
import com.c.infrastructure.dao.McpProtocolHttpDao;
import com.c.infrastructure.dao.McpProtocolMappingDao;
import com.c.infrastructure.dao.po.McpMessageLogPO;
import com.c.infrastructure.dao.po.McpProtocolHttpPO;
import com.c.infrastructure.dao.po.McpProtocolMappingPO;
import com.c.types.enums.MessageStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 协议配置仓储实现
 * 负责协议配置数据持久化、分布式锁控制、本地消息表管理
 * 支撑分布式事务最终一致性与并发安全
 *
 * @author cyh
 * @date 2026/03/29
 */
@Slf4j
@Repository
public class ProtocolRepositoryImpl implements ProtocolRepository {

    /** Redisson分布式锁客户端，用于控制协议配置并发更新 */
    @Resource
    private RedissonClient redissonClient;

    /** HTTP协议配置DAO，操作协议主表 */
    @Resource
    private McpProtocolHttpDao protocolHttpDao;

    /** 协议字段映射DAO，操作协议映射子表 */
    @Resource
    private McpProtocolMappingDao protocolMappingDao;

    /** 本地消息表DAO，用于分布式事务消息重试 */
    @Resource
    private McpMessageLogDao messageLogDao;

    /**
     * 延迟注入自身代理对象
     * 解决内部方法调用@Transactional事务不生效问题
     */
    @Resource
    @Lazy
    private ProtocolRepositoryImpl self;

    /* ========================== 协议核心业务实现 ========================== */

    /**
     * 批量保存协议配置
     * 内部逐条加锁保证并发安全，避免接口路径重复冲突
     *
     * @param protocolVOS 协议视图对象列表
     * @return 生成的协议ID列表
     */
    @Override
    public List<Long> batchSaveProtocols(List<HTTPProtocolVO> protocolVOS) {
        // 空集合直接返回空，避免空指针与无效遍历
        if (null == protocolVOS || protocolVOS.isEmpty()) {
            return Collections.emptyList();
        }

        // 逐条加锁处理，保证最小锁粒度，提升并发安全性
        return protocolVOS
                .stream()
                .map(this::saveWithLock)
                .collect(Collectors.toList());
    }

    /**
     * 加锁保存单条协议配置
     * 锁粒度：HTTP请求方法 + URL，保证唯一接口不被并发修改
     *
     * @param protocolVO 协议视图对象
     * @return 生成的协议ID
     */
    private Long saveWithLock(HTTPProtocolVO protocolVO) {
        // 1. 构造唯一锁Key
        // 建议：对 URL 进行 MD5 或简单的摘要处理，防止 URL 过长超过 Redis Key 限制
        String urlSummary = DigestUtils.md5DigestAsHex(protocolVO
                .getHttpUrl()
                .getBytes(StandardCharsets.UTF_8));
        String lockKey = String.format("mcp:lock:protocol:%s:%s", protocolVO
                .getHttpMethod()
                .toUpperCase(), urlSummary);

        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 2. 尝试获取锁：等待10秒，锁定30秒
            if (!lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                log.warn("并发保存协议冲突，未能获取到锁: {}", lockKey);
                throw new RuntimeException("当前协议正在配置中，请勿重复操作");
            }

            // 3. 获取锁成功，执行核心保存逻辑
            // 注意：这里必须通过 self (Spring 代理对象) 调用，否则 @Transactional 会失效
            return self.saveOrUpdateProtocol(protocolVO);

        } catch (InterruptedException e) {
            Thread
                    .currentThread()
                    .interrupt(); // 恢复中断状态
            log.error("分布式锁操作被中断，Key: {}", lockKey, e);
            throw new RuntimeException("系统操作被中断，请重试");
        } finally {
            // 4. 安全释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("分布式锁已正常释放: {}", lockKey);
            }
        }
    }

    /**
     * 新增或更新协议配置（事务保证）
     * 先删除旧数据，再插入新数据，保证幂等性
     *
     * @param protocolVO 协议视图对象
     * @return 协议ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long saveOrUpdateProtocol(HTTPProtocolVO protocolVO) {
        // 1. 幂等处理：根据URL+方法查询是否已存在配置
        McpProtocolHttpPO oldPO = protocolHttpDao.queryByUrlAndMethod(protocolVO.getHttpUrl(),
                protocolVO.getHttpMethod());
        if (null != oldPO) {
            // 存在旧数据，先删除主表与子表数据，保证数据覆盖一致性
            protocolHttpDao.deleteByProtocolId(oldPO.getProtocolId());
            protocolMappingDao.deleteByProtocolId(oldPO.getProtocolId());
        }

        // 2. 生成全局唯一自增协议ID（Redisson原子自增，保证趋势递增）
        long protocolId = redissonClient
                .getAtomicLong("mcp:seq:protocol_id")
                .incrementAndGet();

        // 3. 构建并插入协议主表数据
        McpProtocolHttpPO httpPO = McpProtocolHttpPO
                .builder()
                .protocolId(protocolId)
                .httpUrl(protocolVO.getHttpUrl())
                .httpMethod(protocolVO.getHttpMethod())
                .httpHeaders(protocolVO.getHttpHeaders())
                .timeout(protocolVO.getTimeout())
                .retryTimes(3)
                .status(ProtocolStatusEnum.ENABLE.getCode())
                .build();
        protocolHttpDao.insert(httpPO);

        // 4. 处理字段映射关系，非空时批量插入子表
        if (null != protocolVO.getMappings() && !protocolVO
                .getMappings()
                .isEmpty()) {
            List<McpProtocolMappingPO> mappingPOs = protocolVO
                    .getMappings()
                    .stream()
                    .map(m -> McpProtocolMappingPO
                            .builder()
                            .protocolId(protocolId)
                            .mappingType(m.getMappingType())
                            .parentPath(m.getParentPath())
                            .fieldName(m.getFieldName())
                            .mcpPath(m.getMcpPath())
                            .mcpType(m.getMcpType())
                            .mcpDescription(m.getMcpDescription())
                            .isRequired(m.getIsRequired())
                            .sortOrder(m.getSortOrder())
                            .build())
                    .collect(Collectors.toList());
            protocolMappingDao.insertList(mappingPOs);
        }

        log.info("协议配置持久化成功 protocolId={} url={}", protocolId, protocolVO.getHttpUrl());
        return protocolId;
    }

    /* ========================== 本地消息表实现 ========================== */

    /**
     * 保存协议刷新消息到本地消息表
     * 用于分布式事务消息重试，保证最终一致性
     *
     * @param message 协议刷新消息
     * @return 生成的消息ID
     */
    @Override
    public String saveMessageLog(ProtocolRefreshMessage message) {
        // 生成全局唯一消息ID，用于幂等去重与状态追踪
        String messageId = "MSG_" + redissonClient
                .getAtomicLong("mcp:seq:message_id")
                .incrementAndGet();
        message.setMessageId(messageId);

        // 构建消息PO对象
        McpMessageLogPO po = McpMessageLogPO
                .builder()
                .messageId(messageId)
                .messageData(JSON.toJSONString(message))
                .status(MessageStatusEnum.WAIT.getCode())
                .retryCount(0)
                .nextRetryTime(new Date())  // 立即执行第一次发送
                .build();

        // 插入消息表
        messageLogDao.insert(po);
        return messageId;
    }

    /**
     * 更新消息状态
     * 发送成功/失败后更新状态
     *
     * @param messageId 消息ID
     * @param status    目标状态
     */
    @Override
    public void updateMessageLogStatus(String messageId, MessageStatusEnum status) {
        messageLogDao.updateStatus(messageId, status.getCode());
    }

    /**
     * 更新消息重试信息
     * 失败后记录重试次数与下次执行时间
     *
     * @param messageId     消息ID
     * @param retryCount    重试次数
     * @param nextRetryTime 下次重试时间
     */
    @Override
    public void updateRetryInfo(String messageId, Integer retryCount, Date nextRetryTime) {
        messageLogDao.updateRetryInfo(messageId, retryCount, nextRetryTime);
    }

    /**
     * 查询待发送/待重试消息
     * 定时任务拉取消息进行补偿
     *
     * @param limit 一次拉取条数
     * @return 消息列表
     */
    @Override
    public List<ProtocolRefreshMessage> queryWaitMessages(Integer limit) {
        List<McpMessageLogPO> poList = messageLogDao.queryWaitMessages(limit);
        if (poList == null || poList.isEmpty()) {
            return Collections.emptyList();
        }

        // PO转领域消息对象，并回填最新重试次数用于退避算法
        return poList
                .stream()
                .map(po -> {
                    ProtocolRefreshMessage msg = JSON.parseObject(po.getMessageData(), ProtocolRefreshMessage.class);
                    msg.setMessageId(po.getMessageId());
                    msg.setRetryCount(po.getRetryCount());
                    return msg;
                })
                .collect(Collectors.toList());
    }

    /* ========================== 标准查询方法 ========================== */

    /**
     * 根据协议ID查询协议详情（主表+子表）
     *
     * @param protocolId 协议ID
     * @return 协议视图对象
     */
    @Override
    public HTTPProtocolVO queryProtocolDetail(Long protocolId) {
        McpProtocolHttpPO httpPO = protocolHttpDao.queryByProtocolId(protocolId);
        if (null == httpPO) {
            return null;
        }

        // 查询关联的字段映射列表
        List<McpProtocolMappingPO> mappingPOList = protocolMappingDao.queryByProtocolId(protocolId);
        return buildHTTPProtocolVO(httpPO, mappingPOList);
    }

    /**
     * 根据URL+请求方法查询协议
     *
     * @param url    请求路径
     * @param method 请求方法
     * @return 协议视图对象
     */
    @Override
    public HTTPProtocolVO queryByUrl(String url, String method) {
        McpProtocolHttpPO httpPO = protocolHttpDao.queryByUrlAndMethod(url, method);
        if (null == httpPO) {
            return null;
        }

        List<McpProtocolMappingPO> mappingPOList = protocolMappingDao.queryByProtocolId(httpPO.getProtocolId());
        return buildHTTPProtocolVO(httpPO, mappingPOList);
    }

    /**
     * 更新协议状态（启用/禁用）
     *
     * @param protocolId 协议ID
     * @param status     目标状态
     */
    @Override
    public void updateStatus(Long protocolId, ProtocolStatusEnum status) {
        protocolHttpDao.updateStatus(protocolId, status.getCode());
    }

    /**
     * 分页查询协议列表（不带映射关系）
     *
     * @param urlKeyword URL关键词
     * @param page       页码
     * @param size       每页条数
     * @return 协议视图对象列表
     */
    @Override
    public List<HTTPProtocolVO> queryProtocolPage(String urlKeyword, Integer page, Integer size) {
        // 计算分页起始位置
        int start = (page - 1) * size;
        List<McpProtocolHttpPO> poList = protocolHttpDao.queryProtocolPage(urlKeyword, start, size);

        if (null == poList || poList.isEmpty()) {
            return new ArrayList<>();
        }

        return poList
                .stream()
                .map(po -> buildHTTPProtocolVO(po, Collections.emptyList()))
                .collect(Collectors.toList());
    }

    /**
     * 查询所有启用状态的协议ID
     *
     * @return 协议ID列表
     */
    @Override
    public List<Long> queryAllActiveProtocolIds() {
        return protocolHttpDao
                .queryAllActive()
                .stream()
                .map(McpProtocolHttpPO::getProtocolId)
                .collect(Collectors.toList());
    }

    /**
     * PO对象转换为HTTPProtocolVO视图对象
     * 统一封装转换逻辑，避免重复代码
     *
     * @param po       协议主表PO
     * @param mappings 协议映射PO列表
     * @return 协议视图对象
     */
    private HTTPProtocolVO buildHTTPProtocolVO(McpProtocolHttpPO po, List<McpProtocolMappingPO> mappings) {
        // 转换字段映射列表
        List<HTTPProtocolVO.ProtocolMapping> mappingVOList = mappings
                .stream()
                .map(m -> HTTPProtocolVO.ProtocolMapping
                        .builder()
                        .mappingType(m.getMappingType())
                        .parentPath(m.getParentPath())
                        .fieldName(m.getFieldName())
                        .mcpPath(m.getMcpPath())
                        .mcpType(m.getMcpType())
                        .mcpDescription(m.getMcpDescription())
                        .isRequired(m.getIsRequired())
                        .sortOrder(m.getSortOrder())
                        .build())
                .collect(Collectors.toList());

        // 构建返回视图对象
        return HTTPProtocolVO
                .builder()
                .protocolId(po.getProtocolId())
                .httpUrl(po.getHttpUrl())
                .httpMethod(po.getHttpMethod())
                .httpHeaders(po.getHttpHeaders())
                .timeout(po.getTimeout())
                .mappings(mappingVOList)
                .build();
    }
}