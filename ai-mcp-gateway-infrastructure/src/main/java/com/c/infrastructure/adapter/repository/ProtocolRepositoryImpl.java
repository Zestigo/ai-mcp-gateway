package com.c.infrastructure.adapter.repository;

import com.c.domain.protocol.adapter.repository.ProtocolRepository;
import com.c.domain.protocol.model.valobj.enums.ProtocolStatusEnum;
import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;
import com.c.infrastructure.dao.McpProtocolHttpDao;
import com.c.infrastructure.dao.McpProtocolMappingDao;
import com.c.infrastructure.dao.po.McpProtocolHttpPO;
import com.c.infrastructure.dao.po.McpProtocolMappingPO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 协议仓储服务实现类
 * 基于分布式锁+Spring事务实现协议的并发安全保存与管理
 * 采用自身注入代理解决类内调用事务失效问题
 *
 * @author cyh
 * @date 2026/03/29
 */
@Slf4j
@Repository
public class ProtocolRepositoryImpl implements ProtocolRepository {

    /** Redisson分布式锁客户端 */
    @Resource
    private RedissonClient redissonClient;

    /** HTTP协议数据访问对象 */
    @Resource
    private McpProtocolHttpDao protocolHttpDao;

    /** 协议字段映射数据访问对象 */
    @Resource
    private McpProtocolMappingDao protocolMappingDao;

    /** 延迟注入自身代理对象，解决类内方法调用事务失效问题 */
    @Resource
    @Lazy
    private ProtocolRepositoryImpl self;

    /**
     * 批量保存协议配置
     * 循环处理单条协议，保证批量操作稳定性
     *
     * @param protocolVOS 协议视图对象集合
     * @return 协议ID集合
     */
    @Override
    public List<Long> batchSaveProtocols(List<HTTPProtocolVO> protocolVOS) {
        List<Long> ids = new ArrayList<>();
        for (HTTPProtocolVO vo : protocolVOS) {
            ids.add(this.saveWithLock(vo));
        }
        return ids;
    }

    /**
     * 分布式锁包装方法，保证协议操作并发安全
     * 锁粒度：请求方法+URL，避免全局锁影响性能
     *
     * @param protocolVO 协议视图对象
     * @return 协议ID
     */
    private Long saveWithLock(HTTPProtocolVO protocolVO) {
        // 构建分布式锁Key，统一大写保证唯一性
        String lockKey = "mcp:lock:protocol:" + protocolVO
                .getHttpMethod()
                .toUpperCase() + ":" + protocolVO.getHttpUrl();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁：最大等待10秒，持有30秒自动释放
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                // 通过自身代理调用，保证@Transactional事务生效
                return self.saveOrUpdateProtocol(protocolVO);
            } else {
                log.warn("获取协议操作锁超时: {}", lockKey);
                throw new RuntimeException("系统繁忙，当前协议正在被操作，请稍后再试");
            }
        } catch (InterruptedException e) {
            // 恢复线程中断状态
            Thread
                    .currentThread()
                    .interrupt();
            throw new RuntimeException("操作被中断", e);
        } finally {
            // 安全释放锁：仅当前线程持有锁时解锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 保存或更新协议核心方法
     * 事务保证原子性，基于URL+Method实现幂等覆盖
     *
     * @param protocolVO 协议视图对象
     * @return 协议ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long saveOrUpdateProtocol(HTTPProtocolVO protocolVO) {
        // 幂等校验：查询相同URL+Method的存量协议
        McpProtocolHttpPO oldPO = protocolHttpDao.queryByUrlAndMethod(protocolVO.getHttpUrl(),
                protocolVO.getHttpMethod());

        // 存在旧数据则物理删除，保证数据干净
        if (null != oldPO) {
            log.info("覆盖现有协议规则: protocolId={}", oldPO.getProtocolId());
            protocolHttpDao.deleteByProtocolId(oldPO.getProtocolId());
            protocolMappingDao.deleteByProtocolId(oldPO.getProtocolId());
        }

        // 生成8位数字协议唯一ID
        long protocolId = Long.parseLong(RandomStringUtils.randomNumeric(8));

        // 构建并插入HTTP协议主数据
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

        // 处理字段映射数据，非空时批量插入
        List<HTTPProtocolVO.ProtocolMapping> mappings = protocolVO.getMappings();
        if (null != mappings && !mappings.isEmpty()) {
            List<McpProtocolMappingPO> mappingPOs = mappings
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

        return protocolId;
    }

    /**
     * 根据协议ID查询完整协议详情
     * 包含主表信息与全量字段映射规则
     *
     * @param protocolId 协议ID
     * @return 协议视图对象
     */
    @Override
    public HTTPProtocolVO queryProtocolDetail(Long protocolId) {
        McpProtocolHttpPO httpPO = protocolHttpDao.queryByProtocolId(protocolId);
        if (null == httpPO) return null;
        List<McpProtocolMappingPO> mappingPOs = protocolMappingDao.queryByProtocolId(protocolId);
        return buildHTTPProtocolVO(httpPO, mappingPOs);
    }

    /**
     * 根据URL和请求方法精确查询协议
     *
     * @param url    请求URL
     * @param method 请求方法
     * @return 协议视图对象
     */
    @Override
    public HTTPProtocolVO queryByUrl(String url, String method) {
        McpProtocolHttpPO httpPO = protocolHttpDao.queryByUrlAndMethod(url, method);
        if (null == httpPO) return null;
        List<McpProtocolMappingPO> mappingPOs = protocolMappingDao.queryByProtocolId(httpPO.getProtocolId());
        return buildHTTPProtocolVO(httpPO, mappingPOs);
    }

    /**
     * 更新协议启用/禁用状态
     *
     * @param protocolId 协议ID
     * @param status     协议状态枚举
     */
    @Override
    public void updateStatus(Long protocolId, ProtocolStatusEnum status) {
        protocolHttpDao.updateStatus(protocolId, status.getCode());
    }

    /**
     * 分页查询协议列表
     * 管理后台使用，不加载映射数据提升性能
     *
     * @param urlKeyword URL模糊关键字
     * @param page       页码
     * @param size       每页条数
     * @return 协议视图对象集合
     */
    @Override
    public List<HTTPProtocolVO> queryProtocolPage(String urlKeyword, Integer page, Integer size) {
        List<McpProtocolHttpPO> pos = protocolHttpDao.queryProtocolPage(urlKeyword, (page - 1) * size, size);
        if (null == pos || pos.isEmpty()) return new ArrayList<>();
        return pos
                .stream()
                .map(po -> buildHTTPProtocolVO(po, new ArrayList<>()))
                .collect(Collectors.toList());
    }

    /**
     * 查询所有启用状态的协议ID
     * 用于定时任务缓存同步
     *
     * @return 协议ID集合
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
     * PO对象转换为VO视图对象
     * 统一数据转换逻辑，提高代码复用性
     *
     * @param po       协议主表PO
     * @param mappings 映射PO集合
     * @return 协议视图对象
     */
    private HTTPProtocolVO buildHTTPProtocolVO(McpProtocolHttpPO po, List<McpProtocolMappingPO> mappings) {
        List<HTTPProtocolVO.ProtocolMapping> mappingVOs = mappings
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
                .httpUrl(po.getHttpUrl())
                .httpMethod(po.getHttpMethod())
                .httpHeaders(po.getHttpHeaders())
                .timeout(po.getTimeout())
                .mappings(mappingVOs)
                .build();
    }
}