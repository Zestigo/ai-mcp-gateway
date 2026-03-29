package com.c.infrastructure.dao;

import com.c.infrastructure.dao.po.McpGatewayToolPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 网关工具配置数据访问接口
 *
 * @author cyh
 * @date 2026/03/29
 */
@Mapper
public interface McpGatewayToolDao {

    /**
     * 插入新的工具配置
     *
     * @param mcpGatewayToolPO 工具配置持久化对象
     * @return 影响行数
     */
    int insert(McpGatewayToolPO mcpGatewayToolPO);

    /**
     * 更新工具基础配置
     *
     * @param mcpGatewayToolPO 工具配置持久化对象
     * @return 影响行数
     */
    int updateToolConfig(McpGatewayToolPO mcpGatewayToolPO);

    /**
     * 更新工具关联的协议信息
     *
     * @param mcpGatewayToolPO 工具配置持久化对象
     * @return 影响行数
     */
    int updateProtocolByGatewayId(McpGatewayToolPO mcpGatewayToolPO);

    /**
     * 根据网关ID查询工具列表
     *
     * @param gatewayId 网关唯一标识
     * @return 工具配置列表
     */
    List<McpGatewayToolPO> queryByGatewayId(String gatewayId);

    /**
     * 根据网关ID和工具名称查询协议ID
     *
     * @param mcpGatewayToolPOReq 查询参数
     * @return 协议ID
     */
    Long queryToolProtocolIdByToolName(McpGatewayToolPO mcpGatewayToolPOReq);

    /**
     * 查询工具配置是否存在
     *
     * @param po 查询参数
     * @return 工具配置对象
     */
    McpGatewayToolPO queryToolConfig(McpGatewayToolPO po);
}