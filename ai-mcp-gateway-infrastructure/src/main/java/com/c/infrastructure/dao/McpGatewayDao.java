package com.c.infrastructure.dao;

import com.c.infrastructure.dao.po.McpGatewayPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MCP网关配置数据访问接口
 *
 * @author cyh
 * @date 2026/03/23
 */
@Mapper
public interface McpGatewayDao {

    /**
     * 新增网关配置
     *
     * @param po 网关配置PO对象
     * @return 受影响行数
     */
    int insert(McpGatewayPO po);

    /**
     * 根据ID删除网关配置
     *
     * @param id 主键ID
     * @return 受影响行数
     */
    int deleteById(Long id);

    /**
     * 根据ID更新网关配置
     *
     * @param po 网关配置PO对象
     * @return 受影响行数
     */
    int updateById(McpGatewayPO po);

    /**
     * 根据ID查询网关配置
     *
     * @param id 主键ID
     * @return 网关配置PO对象
     */
    McpGatewayPO queryById(Long id);

    /**
     * 查询所有网关配置
     *
     * @return 网关配置PO列表
     */
    List<McpGatewayPO> queryAll();

    /**
     * 根据网关ID查询网关配置
     *
     * @param gatewayId 网关唯一标识
     * @return 网关配置PO对象
     */
    McpGatewayPO queryMcpGatewayByGatewayId(String gatewayId);

}