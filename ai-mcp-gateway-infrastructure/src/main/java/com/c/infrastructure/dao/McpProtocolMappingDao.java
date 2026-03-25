package com.c.infrastructure.dao;

import com.c.infrastructure.dao.po.McpProtocolMappingPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MCP协议字段映射数据访问接口
 * 基于MyBatis实现，提供MCP协议字段映射配置的增删改查数据库操作
 *
 * @author cyh
 * @date 2026/03/25
 */
@Mapper
public interface McpProtocolMappingDao {

    /**
     * 新增MCP协议字段映射配置
     *
     * @param po 字段映射持久化对象
     * @return 数据库受影响的行数
     */
    int insert(McpProtocolMappingPO po);

    /**
     * 根据主键ID删除MCP协议字段映射配置
     *
     * @param id 主键ID
     * @return 数据库受影响的行数
     */
    int deleteById(Long id);

    /**
     * 根据主键ID更新MCP协议字段映射配置
     *
     * @param po 字段映射持久化对象
     * @return 数据库受影响的行数
     */
    int updateById(McpProtocolMappingPO po);

    /**
     * 根据主键ID查询单条MCP协议字段映射配置
     *
     * @param id 主键ID
     * @return 字段映射持久化对象
     */
    McpProtocolMappingPO queryById(Long id);

    /**
     * 查询所有MCP协议字段映射配置
     *
     * @return 字段映射持久化对象列表
     */
    List<McpProtocolMappingPO> queryAll();

    /**
     * 根据网关条件查询工具字段映射配置列表
     * 用于获取指定网关下的所有MCP工具字段映射规则
     *
     * @param po 查询条件参数对象
     * @return 字段映射持久化对象列表
     */
    List<McpProtocolMappingPO> queryMcpGatewayToolConfigList(McpProtocolMappingPO po);
}