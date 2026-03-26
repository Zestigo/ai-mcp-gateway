package com.c.infrastructure.dao;

import com.c.infrastructure.dao.po.McpProtocolMappingPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MCP协议字段映射数据访问接口
 *
 * @author cyh
 * @date 2026/03/26
 */
@Mapper
public interface McpProtocolMappingDao {

    /**
     * 新增MCP协议字段映射配置
     *
     * @param po 字段映射持久化对象
     * @return 数据库受影响行数
     */
    int insert(McpProtocolMappingPO po);

    /**
     * 根据主键ID删除MCP协议字段映射配置
     *
     * @param id 主键ID
     * @return 数据库受影响行数
     */
    int deleteById(Long id);

    /**
     * 根据主键ID更新MCP协议字段映射配置
     *
     * @param po 字段映射持久化对象
     * @return 数据库受影响行数
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
     * @return 字段映射持久化对象集合
     */
    List<McpProtocolMappingPO> queryAll();

    /**
     * 根据协议ID查询网关工具配置列表
     *
     * @param protocolId 协议ID
     * @return 协议字段映射配置集合
     */
    List<McpProtocolMappingPO> queryMcpGatewayToolConfigListByProtocolId(Long protocolId);

    List<McpProtocolMappingPO> queryByProtocolIds(@Param("protocolIds") List<Long> protocolIds);
}