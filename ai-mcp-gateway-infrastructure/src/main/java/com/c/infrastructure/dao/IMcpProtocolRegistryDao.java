package com.c.infrastructure.dao;

import com.c.infrastructure.dao.po.McpProtocolRegistryPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MCP工具注册数据访问接口
 *
 * @author cyh
 * @date 2026/03/23
 */
@Mapper
public interface IMcpProtocolRegistryDao {

    /**
     * 新增MCP工具注册配置
     *
     * @param po MCP工具注册PO对象
     * @return 受影响行数
     */
    int insert(McpProtocolRegistryPO po);

    /**
     * 根据ID删除MCP工具注册配置
     *
     * @param id 主键ID
     * @return 受影响行数
     */
    int deleteById(Long id);

    /**
     * 根据ID更新MCP工具注册配置
     *
     * @param po MCP工具注册PO对象
     * @return 受影响行数
     */
    int updateById(McpProtocolRegistryPO po);

    /**
     * 根据ID查询MCP工具注册配置
     *
     * @param id 主键ID
     * @return MCP工具注册PO对象
     */
    McpProtocolRegistryPO queryById(Long id);

    /**
     * 查询所有MCP工具注册配置
     *
     * @return MCP工具注册PO列表
     */
    List<McpProtocolRegistryPO> queryAll();

    /**
     * 根据网关ID查询工具注册配置
     *
     * @param gatewayId 网关唯一标识
     * @return MCP工具注册PO对象
     */
    McpProtocolRegistryPO queryMcpProtocolRegistryByGatewayId(String gatewayId);

}