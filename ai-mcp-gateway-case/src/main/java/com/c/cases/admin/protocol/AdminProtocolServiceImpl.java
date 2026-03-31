package com.c.cases.admin.protocol;

import com.c.cases.admin.AdminProtocolService;
import com.c.domain.admin.adapter.repository.AdminRepository;
import com.c.domain.admin.model.entity.GatewayProtocolEntity;
import com.c.domain.admin.model.valobj.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 管理员协议服务实现类
 * 负责协议配置的管理，包括创建、更新、删除、查询和状态更新等操作
 * 
 * @author cyh
 * @date 2026/03/31
 */
@Service
@RequiredArgsConstructor
public class AdminProtocolServiceImpl implements AdminProtocolService {

    /** 管理员仓库，提供协议配置的持久化操作 */
    private final AdminRepository adminRepository;

    /**
     * 分页查询协议配置
     * 
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @param protocolId 协议ID（可选，用于过滤）
     * @param status 状态（可选，用于过滤）
     * @return 分页响应对象
     */
    @Override
    public PageResponse<GatewayProtocolEntity> queryProtocolConfigPage(int pageNo, int pageSize, Long protocolId, Integer status) {
        return adminRepository.queryProtocolConfigPage(pageNo, pageSize, protocolId, status);
    }

    /**
     * 查询协议配置列表
     * 
     * @param protocolId 协议ID（可选，用于过滤）
     * @param status 状态（可选，用于过滤）
     * @return 协议配置实体列表
     */
    @Override
    public List<GatewayProtocolEntity> getProtocolConfigList(Long protocolId, Integer status) {
        return adminRepository.getProtocolConfigList(protocolId, status);
    }

    /**
     * 创建协议配置
     * 
     * @param entity 协议配置实体
     * @return 创建是否成功
     * @throws IllegalArgumentException 当协议配置为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createProtocol(GatewayProtocolEntity entity) {
        Assert.notNull(entity, "协议配置不能为空");
        return adminRepository.createProtocol(entity);
    }

    /**
     * 更新协议配置
     * 
     * @param entity 协议配置实体
     * @param oldVersion 乐观锁版本号
     * @return 更新是否成功
     * @throws IllegalArgumentException 当参数为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProtocolConfig(GatewayProtocolEntity entity, Long oldVersion) {
        Assert.notNull(entity, "协议配置不能为空");
        Assert.notNull(oldVersion, "乐观锁版本号不能为空");
        return adminRepository.updateProtocolConfigByCas(entity, oldVersion);
    }

    /**
     * 根据ID查询协议配置
     * 
     * @param protocolId 协议ID
     * @return 协议配置实体
     * @throws IllegalArgumentException 当协议ID为空时抛出
     */
    @Override
    public GatewayProtocolEntity findProtocolById(Long protocolId) {
        Assert.notNull(protocolId, "协议ID不能为空");
        return adminRepository.findByProtocolId(protocolId);
    }

    /**
     * 删除协议配置
     * 
     * @param protocolId 协议ID
     * @return 删除是否成功
     * @throws IllegalArgumentException 当协议ID为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteProtocolById(Long protocolId) {
        Assert.notNull(protocolId, "协议ID不能为空");
        return adminRepository.deleteProtocolById(protocolId);
    }

    /**
     * 更新协议状态
     * 
     * @param protocolId 协议ID
     * @param status 状态
     * @param oldVersion 乐观锁版本号
     * @return 更新是否成功
     * @throws IllegalArgumentException 当参数为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProtocolStatus(Long protocolId, Integer status, Long oldVersion) {
        Assert.notNull(protocolId, "协议ID不能为空");
        Assert.notNull(status, "状态不能为空");
        Assert.notNull(oldVersion, "乐观锁版本号不能为空");
        return adminRepository.updateProtocolStatusByCas(protocolId, status, oldVersion);
    }
}