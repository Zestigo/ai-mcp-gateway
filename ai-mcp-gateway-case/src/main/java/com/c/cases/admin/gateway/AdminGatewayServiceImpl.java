package com.c.cases.admin.gateway;

import com.c.cases.admin.AdminGatewayService;
import com.c.domain.admin.adapter.repository.AdminRepository;
import com.c.domain.admin.model.entity.GatewayConfigEntity;
import com.c.domain.admin.model.valobj.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * 管理后台网关配置服务实现类
 * 负责网关的创建、更新、删除、状态管理、分页查询等业务逻辑
 *
 * @author cyh
 * @date 2026/03/31
 */
@Service
@RequiredArgsConstructor
public class AdminGatewayServiceImpl implements AdminGatewayService {

    /** 管理后台数据仓储接口，用于数据持久化操作 */
    private final AdminRepository adminRepository;

    /**
     * 分页查询网关配置列表
     *
     * @param pageNo   当前页码
     * @param pageSize 每页条数
     * @param keyword  搜索关键词
     * @param status   网关状态
     * @return 分页网关配置数据
     */
    @Override
    public PageResponse<GatewayConfigEntity> queryGatewayConfigPage(int pageNo, int pageSize, String keyword,
                                                                    Integer status) {
        return adminRepository.queryGatewayConfigPage(pageNo, pageSize, keyword, status);
    }

    /**
     * 创建网关配置
     *
     * @param entity 网关配置实体对象
     * @return 创建成功返回true，失败返回false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createGateway(GatewayConfigEntity entity) {
        Assert.notNull(entity, "网关配置不能为空");
        Assert.hasText(entity.getGatewayId(), "网关ID不能为空");
        return adminRepository.createGateway(entity);
    }

    /**
     * 更新网关配置（带CAS乐观锁）
     *
     * @param entity     网关配置实体对象
     * @param oldVersion 乐观锁旧版本号
     * @return 更新成功返回true，失败返回false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateGateway(GatewayConfigEntity entity, Long oldVersion) {
        Assert.notNull(entity, "网关配置不能为空");
        Assert.hasText(entity.getGatewayId(), "网关ID不能为空");
        Assert.notNull(oldVersion, "乐观锁版本号不能为空");
        return adminRepository.updateGatewayByCas(entity, oldVersion);
    }

    /**
     * 删除网关配置
     *
     * @param gatewayId 网关唯一标识
     * @return 删除成功返回true，失败返回false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteGateway(String gatewayId) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        return adminRepository.deleteGateway(gatewayId);
    }

    /**
     * 根据网关ID查询网关详情
     *
     * @param gatewayId 网关唯一标识
     * @return 网关配置实体对象
     */
    @Override
    public GatewayConfigEntity findGatewayById(String gatewayId) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        return adminRepository.findGatewayById(gatewayId);
    }

    /**
     * 启用网关（带CAS乐观锁）
     *
     * @param gatewayId  网关唯一标识
     * @param oldVersion 乐观锁旧版本号
     * @return 启用成功返回true，失败返回false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enableGateway(String gatewayId, Long oldVersion) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        Assert.notNull(oldVersion, "乐观锁版本号不能为空");
        return adminRepository.updateGatewayStatusByCas(gatewayId, oldVersion, 1);
    }

    /**
     * 禁用网关（带CAS乐观锁）
     *
     * @param gatewayId  网关唯一标识
     * @param oldVersion 乐观锁旧版本号
     * @return 禁用成功返回true，失败返回false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableGateway(String gatewayId, Long oldVersion) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        Assert.notNull(oldVersion, "乐观锁版本号不能为空");
        return adminRepository.updateGatewayStatusByCas(gatewayId, oldVersion, 0);
    }
}