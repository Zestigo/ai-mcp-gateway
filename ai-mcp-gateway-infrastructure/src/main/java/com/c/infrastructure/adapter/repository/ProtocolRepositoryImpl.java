package com.c.infrastructure.adapter.repository;

import cn.hutool.core.collection.CollUtil;
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
import com.c.types.enums.ResponseCode;
import com.c.types.exception.AppException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 协议仓库实现类
 * 负责协议配置的持久化、查询和更新操作，支持分布式并发控制
 * 
 * @author cyh
 * @date 2026/03/31
 */
@Slf4j
@Repository
public class ProtocolRepositoryImpl implements ProtocolRepository {

    /** 分布式锁键前缀 */
    private static final String LOCK_KEY_PREFIX = "mcp:lock:protocol:";
    /** 协议ID序列键 */
    private static final String PROTOCOL_ID_SEQ_KEY = "mcp:seq:protocol_id";
    /** 消息ID序列键 */
    private static final String MESSAGE_ID_SEQ_KEY = "mcp:seq:message_id";
    /** 锁等待时间（秒） */
    private static final long LOCK_WAIT_TIME = 3;
    /** 锁持有时间（秒） */
    private static final long LOCK_LEASE_TIME = 10;
    /** 默认重试次数 */
    private static final int DEFAULT_RETRY_TIMES = 3;

    /** Redisson客户端，用于分布式锁和序列生成 */
    @Resource
    private RedissonClient redissonClient;

    /** HTTP协议数据访问对象 */
    @Resource
    private McpProtocolHttpDao protocolHttpDao;

    /** 协议映射数据访问对象 */
    @Resource
    private McpProtocolMappingDao protocolMappingDao;

    /** 消息日志数据访问对象 */
    @Resource
    private McpMessageLogDao messageLogDao;

    /** 自引用，用于事务管理 */
    @Resource
    @Lazy
    private ProtocolRepositoryImpl self;

    /**
     * 批量保存协议配置
     * 
     * @param protocolVOS 协议配置列表
     * @return 保存成功的协议ID列表
     */
    @Override
    public List<Long> batchSaveProtocols(List<HTTPProtocolVO> protocolVOS) {
        if (CollUtil.isEmpty(protocolVOS)) {
            return Collections.emptyList();
        }

        return protocolVOS
                .stream()
                .map(this::saveWithLock)
                .collect(Collectors.toList());
    }

    /**
     * 带分布式锁的协议保存
     * 
     * @param protocolVO 协议配置
     * @return 保存成功的协议ID
     */
    private Long saveWithLock(HTTPProtocolVO protocolVO) {
        String urlMd5 = DigestUtils.md5DigestAsHex(protocolVO
                .getHttpUrl()
                .getBytes(StandardCharsets.UTF_8));
        String lockKey = LOCK_KEY_PREFIX + protocolVO
                .getHttpMethod()
                .toUpperCase() + ":" + urlMd5;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean lockSuccess = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!lockSuccess) {
                log.warn("并发保存协议冲突 lockKey:{}", lockKey);
                throw new AppException(ResponseCode.CONCURRENT_ERROR.getCode(), "当前协议正在配置中，请勿重复操作");
            }

            return self.saveOrUpdateProtocol(protocolVO);

        } catch (InterruptedException e) {
            Thread
                    .currentThread()
                    .interrupt();
            log.error("分布式锁被中断 lockKey:{}", lockKey, e);
            throw new AppException(ResponseCode.UN_ERROR.getCode(), "系统操作中断，请重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 保存或更新协议配置
     * 
     * @param protocolVO 协议配置
     * @return 协议ID
     */
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    @Override
    public Long saveOrUpdateProtocol(HTTPProtocolVO protocolVO) {
        McpProtocolHttpPO oldPO = protocolHttpDao.queryByUrlAndMethod(protocolVO.getHttpUrl(),
                protocolVO.getHttpMethod());
        if (oldPO != null) {
            protocolHttpDao.deleteByProtocolId(oldPO.getProtocolId());
            protocolMappingDao.deleteByProtocolId(oldPO.getProtocolId());
        }

        long protocolId = redissonClient
                .getAtomicLong(PROTOCOL_ID_SEQ_KEY)
                .incrementAndGet();

        McpProtocolHttpPO httpPO = McpProtocolHttpPO
                .builder()
                .protocolId(protocolId)
                .httpUrl(protocolVO.getHttpUrl())
                .httpMethod(protocolVO.getHttpMethod())
                .httpHeaders(protocolVO.getHttpHeaders())
                .timeout(protocolVO.getTimeout())
                .retryTimes(DEFAULT_RETRY_TIMES)
                .status(ProtocolStatusEnum.ENABLE.getCode())
                .build();
        protocolHttpDao.createProtocol(httpPO);

        if (CollUtil.isNotEmpty(protocolVO.getMappings())) {
            List<McpProtocolMappingPO> mappingPOList = protocolVO
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
            protocolMappingDao.insertList(mappingPOList);
        }

        log.info("协议保存成功 protocolId:{}", protocolId);
        return protocolId;
    }

    /**
     * 保存协议刷新消息日志
     * 
     * @param message 协议刷新消息
     * @return 消息ID
     */
    @Override
    public String saveMessageLog(ProtocolRefreshMessage message) {
        String messageId = "MSG_" + redissonClient
                .getAtomicLong(MESSAGE_ID_SEQ_KEY)
                .incrementAndGet();
        message.setMessageId(messageId);

        McpMessageLogPO po = McpMessageLogPO
                .builder()
                .messageId(messageId)
                .messageData(JSON.toJSONString(message))
                .status(MessageStatusEnum.WAIT.getCode())
                .retryCount(0)
                .nextRetryTime(new Date())
                .build();

        messageLogDao.insert(po);
        return messageId;
    }

    /**
     * 更新消息日志状态
     * 
     * @param messageId 消息ID
     * @param status 消息状态
     */
    @Override
    public void updateMessageLogStatus(String messageId, MessageStatusEnum status) {
        messageLogDao.updateStatus(messageId, status.getCode());
    }

    /**
     * 更新消息重试信息
     * 
     * @param messageId 消息ID
     * @param retryCount 重试次数
     * @param nextRetryTime 下次重试时间
     */
    @Override
    public void updateRetryInfo(String messageId, Integer retryCount, Date nextRetryTime) {
        messageLogDao.updateRetryInfo(messageId, retryCount, nextRetryTime);
    }

    /**
     * 查询待处理的消息
     * 
     * @param limit 查询数量限制
     * @return 待处理消息列表
     */
    @Override
    public List<ProtocolRefreshMessage> queryWaitMessages(Integer limit) {
        List<McpMessageLogPO> poList = messageLogDao.queryWaitMessages(limit);
        if (CollUtil.isEmpty(poList)) {
            return Collections.emptyList();
        }

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

    /**
     * 查询协议详情
     * 
     * @param protocolId 协议ID
     * @return 协议配置详情
     */
    @Override
    public HTTPProtocolVO queryProtocolDetail(Long protocolId) {
        McpProtocolHttpPO httpPO = protocolHttpDao.queryByProtocolId(protocolId);
        if (httpPO == null) {
            return null;
        }

        List<McpProtocolMappingPO> mappingList = protocolMappingDao.queryByProtocolId(protocolId);
        return buildHTTPProtocolVO(httpPO, mappingList);
    }

    /**
     * 根据URL和方法查询协议
     * 
     * @param url 请求URL
     * @param method 请求方法
     * @return 协议配置
     */
    @Override
    public HTTPProtocolVO queryByUrl(String url, String method) {
        McpProtocolHttpPO httpPO = protocolHttpDao.queryByUrlAndMethod(url, method);
        if (httpPO == null) {
            return null;
        }

        List<McpProtocolMappingPO> mappingList = protocolMappingDao.queryByProtocolId(httpPO.getProtocolId());
        return buildHTTPProtocolVO(httpPO, mappingList);
    }

    /**
     * 更新协议状态
     * 
     * @param protocolId 协议ID
     * @param status 协议状态
     * @throws AppException 当协议不存在或更新冲突时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long protocolId, ProtocolStatusEnum status) {
        McpProtocolHttpPO po = protocolHttpDao.queryByProtocolId(protocolId);
        if (po == null) {
            throw new AppException(ResponseCode.DATA_NOT_FOUND.getCode(), ResponseCode.DATA_NOT_FOUND.getInfo());
        }

        int rows = protocolHttpDao.updateProtocolStatusByCas(protocolId, status.getCode(), po.getVersion());
        if (rows == 0) {
            throw new AppException(ResponseCode.CONCURRENT_ERROR.getCode(), "协议更新冲突，请重试");
        }
    }

    /**
     * 分页查询协议
     * 
     * @param urlKeyword URL关键字
     * @param page 页码
     * @param size 每页大小
     * @return 协议配置列表
     */
    @Override
    public List<HTTPProtocolVO> queryProtocolPage(String urlKeyword, Integer page, Integer size) {
        int start = (page - 1) * size;
        List<McpProtocolHttpPO> poList = protocolHttpDao.queryProtocolPage(urlKeyword, start, size);
        if (CollUtil.isEmpty(poList)) {
            return Collections.emptyList();
        }

        return poList
                .stream()
                .map(po -> buildHTTPProtocolVO(po, Collections.emptyList()))
                .collect(Collectors.toList());
    }

    /**
     * 查询协议数量
     * 
     * @param urlKeyword URL关键字
     * @return 协议数量
     */
    public Long queryProtocolCount(String urlKeyword) {
        return protocolHttpDao.countProtocolPage(urlKeyword);
    }

    /**
     * 查询所有活跃的协议ID
     * 
     * @return 活跃协议ID列表
     */
    @Override
    public List<Long> queryAllActiveProtocolIds() {
        List<McpProtocolHttpPO> activeList = protocolHttpDao.queryAllActive();
        if (CollUtil.isEmpty(activeList)) {
            return Collections.emptyList();
        }
        return activeList
                .stream()
                .map(McpProtocolHttpPO::getProtocolId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 构建HTTP协议VO对象
     * 
     * @param po HTTP协议PO对象
     * @param mappings 协议映射列表
     * @return HTTP协议VO对象
     */
    private HTTPProtocolVO buildHTTPProtocolVO(McpProtocolHttpPO po, List<McpProtocolMappingPO> mappings) {
        List<HTTPProtocolVO.ProtocolMapping> mappingVOList = Optional
                .ofNullable(mappings)
                .orElse(Collections.emptyList())
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