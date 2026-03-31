package com.c.cases.admin.tool;

import com.c.cases.admin.AdminToolService;
import com.c.domain.admin.adapter.repository.AdminRepository;
import com.c.domain.admin.model.entity.GatewayToolConfigEntity;
import com.c.domain.admin.model.valobj.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 管理后台网关工具配置服务实现类
 * 负责网关与MCP工具的绑定、解绑、状态管理、配置更新等业务逻辑
 *
 * @author cyh
 * @date 2026/03/31
 */
@Service
@RequiredArgsConstructor
public class AdminToolServiceImpl implements AdminToolService {

    /** 管理后台数据仓储接口，用于数据持久化操作 */
    private final AdminRepository adminRepository;

    /**
     * 分页查询工具配置列表
     *
     * @param pageNo     当前页码
     * @param pageSize   每页条数
     * @param gatewayId  网关标识
     * @param protocolId 协议标识
     * @param toolStatus 工具状态
     * @return 分页工具配置数据
     */
    @Override
    public PageResponse<GatewayToolConfigEntity> queryToolPage(int pageNo, int pageSize, String gatewayId,
                                                               Long protocolId, Integer toolStatus) {
        return adminRepository.queryToolPage(pageNo, pageSize, gatewayId, protocolId, toolStatus);
    }

    /**
     * 根据网关ID查询关联的所有工具配置
     *
     * @param gatewayId 网关唯一标识
     * @return 工具配置实体列表
     */
    @Override
    public List<GatewayToolConfigEntity> listToolsByGateway(String gatewayId) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        return adminRepository.listToolsByGateway(gatewayId);
    }

    /**
     * 绑定网关与MCP工具
     *
     * @param entity 工具配置实体对象
     * @return 绑定成功返回true，失败返回false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindTool(GatewayToolConfigEntity entity) {
        Assert.notNull(entity, "工具配置不能为空");
        Assert.hasText(entity.getGatewayId(), "网关ID不能为空");
        Assert.notNull(entity.getToolId(), "工具ID不能为空");
        Assert.notNull(entity.getProtocolId(), "协议ID不能为空");
        return adminRepository.bindTool(entity);
    }

    /**
     * 解绑网关与MCP工具
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @return 解绑成功返回true，失败返回false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unbindTool(String gatewayId, Integer toolId, Long protocolId) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        Assert.notNull(toolId, "工具ID不能为空");
        Assert.notNull(protocolId, "协议ID不能为空");
        return adminRepository.unbindTool(gatewayId, toolId, protocolId);
    }

    /**
     * 校验工具是否已绑定
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @return 已绑定返回true，未绑定返回false
     */
    @Override
    public boolean isToolExist(String gatewayId, Integer toolId, Long protocolId) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        Assert.notNull(toolId, "工具ID不能为空");
        Assert.notNull(protocolId, "协议ID不能为空");
        return adminRepository.isToolExist(gatewayId, toolId, protocolId);
    }

    /**
     * 启用网关工具（带CAS乐观锁）
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @param oldVersion 乐观锁旧版本号
     * @return 启用成功返回true，失败返回false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enableTool(String gatewayId, Integer toolId, Long protocolId, Long oldVersion) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        Assert.notNull(toolId, "工具ID不能为空");
        Assert.notNull(protocolId, "协议ID不能为空");
        Assert.notNull(oldVersion, "版本号不能为空");
        return adminRepository.updateToolStatusByCas(gatewayId, toolId, protocolId, 1, oldVersion);
    }

    /**
     * 禁用网关工具（带CAS乐观锁）
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @param oldVersion 乐观锁旧版本号
     * @return 禁用成功返回true，失败返回false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableTool(String gatewayId, Integer toolId, Long protocolId, Long oldVersion) {
        Assert.hasText(gatewayId, "网关ID不能为空");
        Assert.notNull(toolId, "工具ID不能为空");
        Assert.notNull(protocolId, "协议ID不能为空");
        Assert.notNull(oldVersion, "版本号不能为空");
        return adminRepository.updateToolStatusByCas(gatewayId, toolId, protocolId, 0, oldVersion);
    }

    /**
     * 更新工具配置（带CAS乐观锁）
     *
     * @param entity     工具配置实体对象
     * @param oldVersion 乐观锁旧版本号
     * @return 更新成功返回true，失败返回false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateToolConfig(GatewayToolConfigEntity entity, Long oldVersion) {
        Assert.notNull(entity, "工具配置不能为空");
        Assert.hasText(entity.getGatewayId(), "网关ID不能为空");
        Assert.notNull(entity.getToolId(), "工具ID不能为空");
        Assert.notNull(entity.getProtocolId(), "协议ID不能为空");
        Assert.notNull(oldVersion, "版本号不能为空");
        return adminRepository.updateToolConfigByCas(entity, oldVersion);
    }
}