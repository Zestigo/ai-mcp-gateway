package com.c.cases.admin;

import com.c.domain.admin.model.entity.GatewayToolConfigEntity;
import com.c.domain.admin.model.valobj.PageResponse;

import java.util.List;

/**
 * 管理后台网关工具配置服务接口
 * 定义网关与MCP工具绑定相关的业务操作标准
 *
 * @author cyh
 * @date 2026/03/31
 */
public interface AdminToolService {

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
    PageResponse<GatewayToolConfigEntity> queryToolPage(int pageNo, int pageSize, String gatewayId, Long protocolId,
                                                        Integer toolStatus);

    /**
     * 根据网关ID查询关联的所有工具配置
     *
     * @param gatewayId 网关唯一标识
     * @return 工具配置列表
     */
    List<GatewayToolConfigEntity> listToolsByGateway(String gatewayId);

    /**
     * 绑定网关与MCP工具
     *
     * @param entity 工具配置实体
     * @return 绑定结果
     */
    boolean bindTool(GatewayToolConfigEntity entity);

    /**
     * 解绑网关与MCP工具
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @return 解绑结果
     */
    boolean unbindTool(String gatewayId, Integer toolId, Long protocolId);

    /**
     * 校验工具是否已绑定
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @return 校验结果
     */
    boolean isToolExist(String gatewayId, Integer toolId, Long protocolId);

    /**
     * 启用网关工具（带CAS乐观锁）
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @param oldVersion 乐观锁旧版本号
     * @return 启用结果
     */
    boolean enableTool(String gatewayId, Integer toolId, Long protocolId, Long oldVersion);

    /**
     * 禁用网关工具（带CAS乐观锁）
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @param oldVersion 乐观锁旧版本号
     * @return 禁用结果
     */
    boolean disableTool(String gatewayId, Integer toolId, Long protocolId, Long oldVersion);

    /**
     * 更新工具配置（带CAS乐观锁）
     *
     * @param entity     工具配置实体
     * @param oldVersion 乐观锁旧版本号
     * @return 更新结果
     */
    boolean updateToolConfig(GatewayToolConfigEntity entity, Long oldVersion);

}